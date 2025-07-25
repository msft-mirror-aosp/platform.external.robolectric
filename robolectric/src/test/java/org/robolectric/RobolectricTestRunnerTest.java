package org.robolectric;

import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner.RobolectricFrameworkMethod;
import org.robolectric.android.internal.AndroidTestEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.experimental.LazyApplication;
import org.robolectric.annotation.experimental.LazyApplication.LazyLoad;
import org.robolectric.config.ConfigurationRegistry;
import org.robolectric.internal.AndroidSandbox.TestEnvironmentSpec;
import org.robolectric.internal.ShadowProvider;
import org.robolectric.junit.rules.SetSystemPropertyRule;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.pluginapi.Sdk;
import org.robolectric.pluginapi.TestEnvironmentLifecyclePlugin;
import org.robolectric.pluginapi.config.ConfigurationStrategy.Configuration;
import org.robolectric.pluginapi.perf.Metric;
import org.robolectric.pluginapi.perf.PerfStatsReporter;
import org.robolectric.plugins.DefaultSdkPicker;
import org.robolectric.plugins.SdkCollection;
import org.robolectric.util.TempDirectory;
import org.robolectric.util.TestUtil;

@SuppressWarnings("NewApi")
@RunWith(JUnit4.class)
public class RobolectricTestRunnerTest {

  private RunNotifier notifier;
  private List<String> events;
  private String priorEnabledSdks;
  private String priorAlwaysInclude;
  private SdkCollection sdkCollection;

  @Rule public SetSystemPropertyRule setSystemPropertyRule = new SetSystemPropertyRule();

  @Before
  public void setUp() throws Exception {
    notifier = new RunNotifier();
    events = new ArrayList<>();
    notifier.addListener(new MyRunListener());

    priorEnabledSdks = System.getProperty("robolectric.enabledSdks");
    System.clearProperty("robolectric.enabledSdks");

    priorAlwaysInclude = System.getProperty("robolectric.alwaysIncludeVariantMarkersInTestName");
    System.clearProperty("robolectric.alwaysIncludeVariantMarkersInTestName");

    sdkCollection = TestUtil.getSdkCollection();
  }

  @After
  public void tearDown() throws Exception {
    TestUtil.resetSystemProperty(
        "robolectric.alwaysIncludeVariantMarkersInTestName", priorAlwaysInclude);
    TestUtil.resetSystemProperty("robolectric.enabledSdks", priorEnabledSdks);
  }

  @Test
  public void ignoredTestCanSpecifyUnsupportedSdkWithoutExploding() throws Exception {
    RobolectricTestRunner runner =
        new RobolectricTestRunner(
            TestWithOldSdk.class,
            org.robolectric.RobolectricTestRunner.defaultInjector()
                .bind(org.robolectric.pluginapi.SdkPicker.class, AllEnabledSdkPicker.class)
                .build());
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: oldSdkMethod",
            "failure: API level 11 is not available",
            "finished: oldSdkMethod",
            "ignored: ignoredOldSdkMethod");
  }

  @Test
  public void failureInResetterDoesntBreakAllTests() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(
            TestWithTwoMethods.class,
            SingleSdkRobolectricTestRunner.defaultInjector()
                .bind(
                    TestEnvironmentSpec.class,
                    new TestEnvironmentSpec(AndroidTestEnvironmentWithFailingSetUp.class))
                .build());
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: first",
            "failure: fake error in setUpApplicationState",
            "finished: first",
            "started: second",
            "failure: fake error in setUpApplicationState",
            "finished: second")
        .inOrder();
  }

  @Test
  public void noClassDefError_isReplacedByBetterLinkageError() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(
            TestWithTwoMethods.class,
            SingleSdkRobolectricTestRunner.defaultInjector()
                .bind(
                    TestEnvironmentSpec.class,
                    new TestEnvironmentSpec(AndroidTestEnvironmentThrowsLinkageError.class))
                .build());
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: first",
            "failure: java.lang.ExceptionInInitializerError",
            "finished: first",
            "started: second",
            "failure: java.lang.ExceptionInInitializerError",
            "finished: second")
        .inOrder();
  }

  @Test
  public void failureInAppOnCreateDoesntBreakAllTests() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(TestWithBrokenAppCreate.class);
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: first",
            "failure: fake error in application.onCreate",
            "finished: first",
            "started: second",
            "failure: fake error in application.onCreate",
            "finished: second")
        .inOrder();
  }

  @Test
  public void failureInAppOnTerminateDoesntBreakAllTests() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(TestWithBrokenAppTerminate.class);
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: first",
            "failure: fake error in application.onTerminate",
            "finished: first",
            "started: second",
            "failure: fake error in application.onTerminate",
            "finished: second")
        .inOrder();
  }

  @Test
  public void equalityOfRobolectricFrameworkMethod() throws Exception {
    Method method = TestWithTwoMethods.class.getMethod("first");
    RobolectricFrameworkMethod rfm16 =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkCollection.getSdk(16),
            mock(Configuration.class),
            false);
    RobolectricFrameworkMethod rfm17 =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkCollection.getSdk(17),
            mock(Configuration.class),
            false);
    RobolectricFrameworkMethod rfm16b =
        new RobolectricFrameworkMethod(
            method,
            mock(AndroidManifest.class),
            sdkCollection.getSdk(16),
            mock(Configuration.class),
            false);

    assertThat(rfm16).isNotEqualTo(rfm17);
    assertThat(rfm16).isEqualTo(rfm16b);

    assertThat(rfm16.hashCode()).isEqualTo(rfm16b.hashCode());
  }

  @Test
  public void shouldReportPerfStats() throws Exception {
    List<Metric> metrics = new ArrayList<>();
    PerfStatsReporter reporter = (metadata, metrics1) -> metrics.addAll(metrics1);

    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(
            TestWithTwoMethods.class,
            SingleSdkRobolectricTestRunner.defaultInjector()
                .bind(PerfStatsReporter[].class, new PerfStatsReporter[] {reporter})
                .build());

    runner.run(notifier);

    Set<String> metricNames = metrics.stream().map(Metric::getName).collect(toSet());
    assertThat(metricNames).contains("initialization");
  }

  @Test
  public void failedTest_shouldStillReportPerfStats() throws Exception {
    List<Metric> metrics = new ArrayList<>();
    PerfStatsReporter reporter = (metadata, metrics1) -> metrics.addAll(metrics1);

    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(
            TestThatFails.class,
            SingleSdkRobolectricTestRunner.defaultInjector()
                .bind(PerfStatsReporter[].class, new PerfStatsReporter[] {reporter})
                .build());

    runner.run(notifier);

    Set<String> metricNames = metrics.stream().map(Metric::getName).collect(toSet());
    assertThat(metricNames).contains("initialization");
  }

  @Test
  public void shouldResetThreadInterrupted() throws Exception {
    RobolectricTestRunner runner = new SingleSdkRobolectricTestRunner(TestWithInterrupt.class);
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: first",
            "finished: first",
            "started: second",
            "failure: failed for the right reason",
            "finished: second");
  }

  @Test
  public void shouldDiagnoseUnexecutedRunnables() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(TestWithUnexecutedRunnables.class);
    runner.run(notifier);
    assertThat(events)
        .containsExactly(
            "started: failWithNoRunnables",
            "failure: failing with no runnables",
            "finished: failWithNoRunnables",
            "started: failWithUnexecutedRunnables",
            "failure: failing with unexecuted runnable\n"
                + "Suppressed: Main looper has queued unexecuted runnables. "
                + "This might be the cause of the test failure. "
                + "You might need a shadowOf(Looper.getMainLooper()).idle() call.",
            "finished: failWithUnexecutedRunnables",
            "started: assumptionViolationWithNoRunnables",
            "ignored: assumptionViolationWithNoRunnables: assumption violated",
            "finished: assumptionViolationWithNoRunnables",
            "started: assumptionViolationWithUnexecutedRunnables",
            "ignored: assumptionViolationWithUnexecutedRunnables: assumption violated",
            "finished: assumptionViolationWithUnexecutedRunnables");
  }

  /////////////////////////////

  /** To simulate failures. */
  public static class AndroidTestEnvironmentWithFailingSetUp extends AndroidTestEnvironment {

    public AndroidTestEnvironmentWithFailingSetUp(
        @Named("runtimeSdk") Sdk runtimeSdk,
        @Named("compileSdk") Sdk compileSdk,
        ShadowProvider[] shadowProviders,
        TestEnvironmentLifecyclePlugin[] lifecyclePlugins) {
      super(runtimeSdk, compileSdk, shadowProviders, lifecyclePlugins);
    }

    @Override
    public void setUpApplicationState(
        String tmpDirName, Configuration configuration, AndroidManifest appManifest) {
      // ConfigurationRegistry.instance is required for resetters.
      ConfigurationRegistry.instance = new ConfigurationRegistry(configuration.map());
      throw new RuntimeException("fake error in setUpApplicationState");
    }
  }

  public static class AndroidTestEnvironmentThrowsLinkageError extends AndroidTestEnvironment {

    public static final class UnloadableClass {
      static {
        if (true) {
          throw new RuntimeException("error in static initializer");
        }
      }

      public static void doStuff() {}

      private UnloadableClass() {}
    }

    public AndroidTestEnvironmentThrowsLinkageError(
        @Named("runtimeSdk") Sdk runtimeSdk,
        @Named("compileSdk") Sdk compileSdk,
        ShadowProvider[] shadowProviders,
        TestEnvironmentLifecyclePlugin[] lifecyclePlugins) {
      super(runtimeSdk, compileSdk, shadowProviders, lifecyclePlugins);
    }

    @Override
    public void setUpApplicationState(
        String tmpDirName, Configuration configuration, AndroidManifest appManifest) {
      UnloadableClass.doStuff();
    }

    @Override
    public void resetState() {}
  }

  @Ignore
  public static class TestWithOldSdk {
    @Config(sdk = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void oldSdkMethod() throws Exception {
      fail("I should not be run!");
    }

    @Ignore("This test shouldn't run, and shouldn't cause the test runner to fail")
    @Config(sdk = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void ignoredOldSdkMethod() throws Exception {
      fail("I should not be run!");
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  @Config(qualifiers = "w123dp-h456dp-land-hdpi")
  public static class TestWithTwoMethods {
    @Test
    public void first() throws Exception {}

    @Test
    public void second() throws Exception {}
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class TestThatFails {
    @Test
    public void first() throws Exception {
      throw new AssertionError();
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  @Config(application = TestWithBrokenAppCreate.MyTestApplication.class)
  @LazyApplication(LazyLoad.OFF)
  public static class TestWithBrokenAppCreate {
    @Test
    public void first() throws Exception {}

    @Test
    public void second() throws Exception {}

    public static class MyTestApplication extends Application {
      @SuppressLint("MissingSuperCall")
      @Override
      public void onCreate() {
        throw new RuntimeException("fake error in application.onCreate");
      }
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  @Config(application = TestWithBrokenAppTerminate.MyTestApplication.class)
  @LazyApplication(LazyLoad.OFF)
  public static class TestWithBrokenAppTerminate {
    @Test
    public void first() throws Exception {}

    @Test
    public void second() throws Exception {}

    public static class MyTestApplication extends Application {
      @SuppressLint("MissingSuperCall")
      @Override
      public void onTerminate() {
        throw new RuntimeException("fake error in application.onTerminate");
      }
    }
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class TestWithInterrupt {
    @Test
    public void first() throws Exception {
      Thread.currentThread().interrupt();
    }

    @Test
    public void second() throws Exception {
      TempDirectory tempDirectory = new TempDirectory("test");

      try {
        Path jarPath = tempDirectory.create("some-jar").resolve("some.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
          out.putNextEntry(new JarEntry("README.txt"));
          out.write("hi!".getBytes(StandardCharsets.UTF_8));
        }

        FileSystemProvider jarFSP =
            FileSystemProvider.installedProviders().stream()
                .filter(p -> p.getScheme().equals("jar"))
                .findFirst()
                .get();
        Path fakeJarFile = Paths.get(jarPath.toUri());

        // if Thread.interrupted() was true, this would fail in AbstractInterruptibleChannel:
        jarFSP.newFileSystem(fakeJarFile, new HashMap<>());
      } finally {
        tempDirectory.destroy();
      }

      fail("failed for the right reason");
    }
  }

  /** Fixture for #shouldDiagnoseUnexecutedRunnables() */
  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class TestWithUnexecutedRunnables {

    @Test
    public void failWithUnexecutedRunnables() {
      shadowMainLooper().pause();
      new Handler(Looper.getMainLooper()).post(() -> {});
      fail("failing with unexecuted runnable");
    }

    @Test
    public void failWithNoRunnables() {
      fail("failing with no runnables");
    }

    @Test
    public void assumptionViolationWithUnexecutedRunnables() {
      shadowMainLooper().pause();
      new Handler(Looper.getMainLooper()).post(() -> {});
      throw new AssumptionViolatedException("assumption violated");
    }

    @Test
    public void assumptionViolationWithNoRunnables() {
      throw new AssumptionViolatedException("assumption violated");
    }
  }

  /** Ignore the value of --Drobolectric.enabledSdks */
  public static class AllEnabledSdkPicker extends DefaultSdkPicker {
    @Inject
    public AllEnabledSdkPicker(@Nonnull SdkCollection sdkCollection) {
      super(sdkCollection, (String) null);
    }
  }

  private class MyRunListener extends RunListener {

    @Override
    public void testRunStarted(Description description) {
      events.add("run started: " + description.getMethodName());
    }

    @Override
    public void testRunFinished(Result result) {
      events.add("run finished: " + result);
    }

    @Override
    public void testStarted(Description description) {
      events.add("started: " + description.getMethodName());
    }

    @Override
    public void testFinished(Description description) {
      events.add("finished: " + description.getMethodName());
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
      events.add(
          "ignored: " + failure.getDescription().getMethodName() + ": " + failure.getMessage());
    }

    @Override
    public void testIgnored(Description description) {
      events.add("ignored: " + description.getMethodName());
    }

    @Override
    public void testFailure(Failure failure) {
      Throwable exception = failure.getException();
      String message = exception.getMessage();
      if (message == null) {
        message = exception.toString();
      }
      for (Throwable suppressed : exception.getSuppressed()) {
        message += "\nSuppressed: " + suppressed.getMessage();
      }
      events.add("failure: " + message);
    }
  }

  @Test
  public void shouldReportExceptionsInBeforeClass() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(TestWithBeforeClassThatThrowsRuntimeException.class);
    runner.run(notifier);
    if (Boolean.getBoolean("robolectric.useLegacySandboxFlow")) {
      assertThat(events.get(1)).startsWith("failure: fail");
    } else {
      assertThat(events.get(0)).isEqualTo("failure: fail");
    }
  }

  @Ignore
  public static class TestWithBeforeClassThatThrowsRuntimeException {
    @BeforeClass
    public static void beforeClass() {
      throw new RuntimeException("fail");
    }

    @Test
    public void test() {}
  }

  @Test
  public void shouldInvokeAfterClass() throws Exception {
    RobolectricTestRunner runner =
        new SingleSdkRobolectricTestRunner(TestClassWithAfterClass.class);
    setSystemPropertyRule.set("RobolectricTestRunnerTest.wasAfterClassCalled", "false");
    runner.run(notifier);
    assertThat(System.getProperty("RobolectricTestRunnerTest.wasAfterClassCalled"))
        .isEqualTo("true");
  }

  @Ignore
  public static class TestClassWithAfterClass {
    @AfterClass
    public static void afterClass() {
      System.setProperty("RobolectricTestRunnerTest.wasAfterClassCalled", "true");
    }

    @Test
    public void test() {}
  }

  @Ignore
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class TestWithIgnore {
    @Test
    public void test() {}

    // to verify @Ignore behavior
    @Ignore
    @Test
    public void ignoredTest() {}
  }

  @Test
  public void shouldNotifyIgnoredTests() throws Exception {
    RobolectricTestRunner runner = new SingleSdkRobolectricTestRunner(TestWithIgnore.class);
    runner.run(notifier);
    assertThat(events)
        .containsExactly("ignored: ignoredTest", "started: test", "finished: test")
        .inOrder();
  }
}
