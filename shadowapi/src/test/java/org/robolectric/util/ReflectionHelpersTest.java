package org.robolectric.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.annotation.ClassName;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@RunWith(JUnit4.class)
public class ReflectionHelpersTest {

  @Test
  public void hasConstructor() {
    assertThat(ReflectionHelpers.hasConstructor(ExampleClass.class, String.class)).isTrue();
    assertThat(ReflectionHelpers.hasConstructor(ExampleClass.class, int.class)).isTrue();
    assertThat(ReflectionHelpers.hasConstructor(ExampleClass.class, int.class, int.class))
        .isFalse();
    assertThat(ReflectionHelpers.hasConstructor(ExampleClass.class, double.class)).isFalse();
    assertThat(ReflectionHelpers.hasConstructor(ExampleClass.class, Object.class)).isFalse();
  }

  @Test
  public void hasMethod() {
    assertThat(ReflectionHelpers.hasMethod(ExampleClass.class, "setName", String.class)).isTrue();
    assertThat(ReflectionHelpers.hasMethod(ExampleClass.class, "setName", Integer.class)).isFalse();
    assertThat(ReflectionHelpers.hasMethod(ExampleClass.class, "setFoo", String.class)).isFalse();
    assertThat(ReflectionHelpers.hasMethod(ExampleClass.class, "getName")).isTrue();
    assertThat(ReflectionHelpers.hasMethod(ExampleClass.class, "getName", Integer.class)).isFalse();
  }

  @Test
  public void getFieldReflectively_getsPrivateFields() {
    ExampleDescendant example = new ExampleDescendant();
    example.overridden = 5;
    assertThat((int) ReflectionHelpers.getField(example, "overridden")).isEqualTo(5);
  }

  @Test
  public void getFieldReflectively_getsInheritedFields() {
    ExampleDescendant example = new ExampleDescendant();
    example.setNotOverridden(6);
    assertThat((int) ReflectionHelpers.getField(example, "notOverridden")).isEqualTo(6);
  }

  @Test
  public void getFieldReflectively_givesHelpfulExceptions() {
    ExampleDescendant example = new ExampleDescendant();
    try {
      ReflectionHelpers.getField(example, "nonExistent");
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      if (!e.getMessage().contains("nonExistent")) {
        throw new RuntimeException("Incorrect exception thrown", e);
      }
    }
  }

  @Test
  public void setFieldReflectively_setsPrivateFields() {
    ExampleDescendant example = new ExampleDescendant();
    example.overridden = 5;
    ReflectionHelpers.setField(example, "overridden", 10);
    assertThat(example.overridden).isEqualTo(10);
  }

  @Test
  public void setFieldReflectively_setsInheritedFields() {
    ExampleDescendant example = new ExampleDescendant();
    example.setNotOverridden(5);
    ReflectionHelpers.setField(example, "notOverridden", 10);
    assertThat(example.getNotOverridden()).isEqualTo(10);
  }

  @Test
  public void setFieldReflectively_givesHelpfulExceptions() {
    ExampleDescendant example = new ExampleDescendant();
    try {
      ReflectionHelpers.setField(example, "nonExistent", 6);
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      if (!e.getMessage().contains("nonExistent")) {
        throw new RuntimeException("Incorrect exception thrown", e);
      }
    }
  }

  @Test
  public void getStaticFieldReflectively_withField_getsStaticField() throws Exception {
    Field field = ExampleDescendant.class.getDeclaredField("DESCENDANT");

    int result = ReflectionHelpers.getStaticField(field);
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void getStaticFieldReflectively_withFieldName_getsStaticField() {
    assertThat((int) ReflectionHelpers.getStaticField(ExampleDescendant.class, "DESCENDANT"))
        .isEqualTo(6);
  }

  @Test
  public void getFinalStaticFieldReflectively_withField_getsStaticField() throws Exception {
    Field field = ExampleBase.class.getDeclaredField("BASE");

    int result = ReflectionHelpers.getStaticField(field);
    assertThat(result).isEqualTo(8);
  }

  @Test
  public void getFinalStaticFieldReflectively_withFieldName_getsStaticField() throws Exception {
    assertThat((int) ReflectionHelpers.getStaticField(ExampleBase.class, "BASE")).isEqualTo(8);
  }

  @Test
  public void setStaticFieldReflectively_withField_setsStaticFields() throws Exception {
    Field field = ExampleDescendant.class.getDeclaredField("DESCENDANT");
    int startingValue = ReflectionHelpers.getStaticField(field);

    ReflectionHelpers.setStaticField(field, 7);
    assertWithMessage("startingValue").that(startingValue).isEqualTo(6);
    assertWithMessage("DESCENDENT").that(ExampleDescendant.DESCENDANT).isEqualTo(7);

    /// Reset the value to avoid test pollution
    ReflectionHelpers.setStaticField(field, startingValue);
  }

  @Test
  public void setStaticFieldReflectively_withFieldName_setsStaticFields() {
    int startingValue = ReflectionHelpers.getStaticField(ExampleDescendant.class, "DESCENDANT");

    ReflectionHelpers.setStaticField(ExampleDescendant.class, "DESCENDANT", 7);
    assertWithMessage("startingValue").that(startingValue).isEqualTo(6);
    assertWithMessage("DESCENDENT").that(ExampleDescendant.DESCENDANT).isEqualTo(7);

    // Reset the value to avoid test pollution
    ReflectionHelpers.setStaticField(ExampleDescendant.class, "DESCENDANT", startingValue);
  }

  @Test
  public void setFinalStaticFieldReflectively_withFieldName_setsStaticFields() {
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> ReflectionHelpers.setStaticField(ExampleWithFinalStatic.class, "FIELD", 101));
    assertThat(thrown)
        .hasCauseThat()
        .hasMessageThat()
        .contains("Cannot set the value of final field");
  }

  @Test
  public void callInstanceMethodReflectively_callsPrivateMethods() {
    ExampleDescendant example = new ExampleDescendant();
    assertThat((int) ReflectionHelpers.callInstanceMethod(example, "returnNumber")).isEqualTo(1337);
  }

  @Test
  public void
      callInstanceMethodReflectively_whenMultipleSignaturesExistForAMethodName_callsMethodWithCorrectSignature() {
    ExampleDescendant example = new ExampleDescendant();
    int returnNumber =
        ReflectionHelpers.callInstanceMethod(
            example, "returnNumber", ClassParameter.from(int.class, 5));
    assertThat(returnNumber).isEqualTo(5);
  }

  @Test
  public void callInstanceMethodReflectively_callsInheritedMethods() {
    ExampleDescendant example = new ExampleDescendant();
    assertThat((int) ReflectionHelpers.callInstanceMethod(example, "returnNegativeNumber"))
        .isEqualTo(-46);
  }

  @Test
  public void callInstanceMethodReflectively_givesHelpfulExceptions() {
    ExampleDescendant example = new ExampleDescendant();
    try {
      ReflectionHelpers.callInstanceMethod(example, "nonExistent");
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      if (!e.getMessage().contains("nonExistent")) {
        throw new RuntimeException("Incorrect exception thrown", e);
      }
    }
  }

  @Test
  public void callInstanceMethodReflectively_rethrowsUncheckedException() {
    ExampleDescendant example = new ExampleDescendant();
    try {
      ReflectionHelpers.callInstanceMethod(example, "throwUncheckedException");
      fail("Expected exception not thrown");
    } catch (TestRuntimeException e) {
    } catch (RuntimeException e) {
      throw new RuntimeException("Incorrect exception thrown", e);
    }
  }

  @Test
  public void callInstanceMethodReflectively_rethrowsError() {
    ExampleDescendant example = new ExampleDescendant();
    try {
      ReflectionHelpers.callInstanceMethod(example, "throwError");
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      throw new RuntimeException("Incorrect exception thrown", e);
    } catch (TestError e) {
    }
  }

  @Test
  public void callInstanceMethodReflectively_wrapsCheckedException() {
    ExampleDescendant example = new ExampleDescendant();
    try {
      ReflectionHelpers.callInstanceMethod(example, "throwCheckedException");
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(TestException.class);
    }
  }

  @Test
  public void callStaticMethodReflectively_callsPrivateStaticMethodsReflectively() {
    int constantNumber =
        ReflectionHelpers.callStaticMethod(ExampleDescendant.class, "getConstantNumber");
    assertThat(constantNumber).isEqualTo(1);
  }

  @Test
  public void callStaticMethodReflectively_rethrowsUncheckedException() {
    try {
      ReflectionHelpers.callStaticMethod(ExampleDescendant.class, "staticThrowUncheckedException");
      fail("Expected exception not thrown");
    } catch (TestRuntimeException e) {
    } catch (RuntimeException e) {
      throw new RuntimeException("Incorrect exception thrown", e);
    }
  }

  @Test
  public void callStaticMethodReflectively_rethrowsError() {
    try {
      ReflectionHelpers.callStaticMethod(ExampleDescendant.class, "staticThrowError");
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      throw new RuntimeException("Incorrect exception thrown", e);
    } catch (TestError e) {
    }
  }

  @Test
  public void callStaticMethodReflectively_wrapsCheckedException() {
    try {
      ReflectionHelpers.callStaticMethod(ExampleDescendant.class, "staticThrowCheckedException");
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(TestException.class);
    }
  }

  @Test
  public void callConstructorReflectively_callsPrivateConstructors() {
    ExampleClass e = ReflectionHelpers.callConstructor(ExampleClass.class);
    assertThat(e).isNotNull();
  }

  @Test
  public void callConstructorReflectively_rethrowsUncheckedException() {
    try {
      ReflectionHelpers.callConstructor(ThrowsUncheckedException.class);
      fail("Expected exception not thrown");
    } catch (TestRuntimeException e) {
    } catch (RuntimeException e) {
      throw new RuntimeException("Incorrect exception thrown", e);
    }
  }

  @Test
  public void callConstructorReflectively_rethrowsError() {
    try {
      ReflectionHelpers.callConstructor(ThrowsError.class);
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      throw new RuntimeException("Incorrect exception thrown", e);
    } catch (TestError e) {
    }
  }

  @Test
  public void callConstructorReflectively_wrapsCheckedException() {
    try {
      ReflectionHelpers.callConstructor(ThrowsCheckedException.class);
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(TestException.class);
    }
  }

  @Test
  public void
      callConstructorReflectively_whenMultipleSignaturesExistForTheConstructor_callsConstructorWithCorrectSignature() {
    ExampleClass ec =
        ReflectionHelpers.callConstructor(ExampleClass.class, ClassParameter.from(int.class, 16));
    assertWithMessage("index").that(ec.index).isEqualTo(16);
    assertWithMessage("name").that(ec.name).isNull();
  }

  @Test
  public void callHasField_withstaticandregularmember() {
    assertWithMessage("has field failed for member: unusedName")
        .that(ReflectionHelpers.hasField(FieldTestClass.class, "unusedName"))
        .isTrue();
    assertWithMessage("has field failed for member: unusedStaticName")
        .that(ReflectionHelpers.hasField(FieldTestClass.class, "unusedStaticName"))
        .isTrue();
    assertWithMessage("has field failed for non existant member: noname")
        .that(ReflectionHelpers.hasField(FieldTestClass.class, "noname"))
        .isFalse();
  }

  @Test
  public void createDelegatingProxy_defersToNullProxyIfNoMethodMatches() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Object());
    assertThat(fixture.delegateMethod()).isNull();
  }

  @Test
  public void createDelegatingProxy_defersToDelegate() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Delegate());
    assertThat(fixture.delegateMethod()).isEqualTo("called");
  }

  @Test
  public void createDelegatingProxy_defersToDelegateWithParams() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Delegate());
    assertThat(fixture.delegateMethod("value")).isEqualTo("called value");
  }

  @Test
  public void createDelegatingProxy_wrongParamType() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Delegate());
    // verify the mismatched delegate method doesn't get matched
    assertThat(fixture.delegateMethodWrongParamType("value")).isNull();
  }

  @Test
  public void createDelegatingProxy_wrongVisibility() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Delegate());
    // verify the mismatched delegate method doesn't get matched
    assertThat(fixture.delegateMethodWrongVisibility("value")).isNull();
  }

  @Test
  public void createDelegatingProxy_className() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Delegate());
    assertThat(fixture.delegateMethodWithClassName("value")).isEqualTo("called ClassName value");
  }

  @Test
  public void createDelegatingProxy_multipleParams() {
    DelegatingProxyFixture fixture =
        ReflectionHelpers.createDelegatingProxy(DelegatingProxyFixture.class, new Delegate());
    assertThat(fixture.delegateMethod("value", "value2")).isEqualTo("called valuevalue2");
  }

  @SuppressWarnings("serial")
  private static class TestError extends Error {}

  @SuppressWarnings("serial")
  private static class TestException extends Exception {}

  @SuppressWarnings("serial")
  private static class TestRuntimeException extends RuntimeException {}

  @SuppressWarnings("unused")
  private static class ExampleBase {
    private int notOverridden;
    protected int overridden;

    private static final int BASE = 8;

    public int getNotOverridden() {
      return notOverridden;
    }

    public void setNotOverridden(int notOverridden) {
      this.notOverridden = notOverridden;
    }

    private int returnNegativeNumber() {
      return -46;
    }
  }

  @SuppressWarnings("unused")
  private static class ExampleDescendant extends ExampleBase {

    public static int DESCENDANT = 6;

    @SuppressWarnings("HidingField")
    protected int overridden;

    private int returnNumber() {
      return 1337;
    }

    private int returnNumber(int n) {
      return n;
    }

    private static int getConstantNumber() {
      return 1;
    }

    private void throwUncheckedException() {
      throw new TestRuntimeException();
    }

    private void throwCheckedException() throws Exception {
      throw new TestException();
    }

    private void throwError() {
      throw new TestError();
    }

    private static void staticThrowUncheckedException() {
      throw new TestRuntimeException();
    }

    private static void staticThrowCheckedException() throws Exception {
      throw new TestException();
    }

    private static void staticThrowError() {
      throw new TestError();
    }
  }

  @SuppressWarnings("unused")
  private static class ExampleWithFinalStatic {
    private static final int FIELD = 100;
  }

  private static class ThrowsError {
    @SuppressWarnings("unused")
    public ThrowsError() {
      throw new TestError();
    }
  }

  private static class ThrowsCheckedException {
    @SuppressWarnings("unused")
    public ThrowsCheckedException() throws Exception {
      throw new TestException();
    }
  }

  private static class ThrowsUncheckedException {
    @SuppressWarnings("unused")
    public ThrowsUncheckedException() {
      throw new TestRuntimeException();
    }
  }

  private static class ExampleClass {
    public String name;
    public int index;

    private ExampleClass() {}

    private ExampleClass(String name) {
      this.name = name;
    }

    private ExampleClass(int index) {
      this.index = index;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class FieldTestClass {
    public String unusedName;
    public static String unusedStaticName = "unusedStaticNameValue";

    private FieldTestClass() {}
  }

  private interface DelegatingProxyFixture {
    String delegateMethod();

    String delegateMethod(String value);

    String delegateMethod(String value, String value2);

    String delegateMethodWrongParamType(String value);

    String delegateMethodWithClassName(String value);

    String delegateMethodWrongVisibility(String value);
  }

  /** A delegate for DelegatingProxyFixture */
  private static class Delegate {
    public String delegateMethod() {
      return "called";
    }

    public String delegateMethod(String value) {
      return "called " + value;
    }

    public String delegateMethod(String value, String value2) {
      return "called " + value + value2;
    }

    public String delegateMethodWrongParamType(int value) {
      throw new IllegalStateException("delegateMethodWrongParamType unexpectedly called");
    }

    /**
     * Add a Nullable annotation as well as ClassName to ensure logic handles multiple annotations
     */
    public String delegateMethodWithClassName(
        @Nullable @ClassName("java.lang.String") Object value) {
      return "called ClassName " + (String) value;
    }

    String delegateMethodWrongVisibility(String value) {
      throw new IllegalStateException("delegateMethodWrongVisibility unexpectedly called");
    }
  }
}
