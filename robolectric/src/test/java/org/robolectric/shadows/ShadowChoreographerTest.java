package org.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.animation.ObjectAnimator;
import android.os.Looper;
import android.view.Choreographer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.junit.rules.SetSystemPropertyRule;
import org.robolectric.shadow.api.Shadow;

/** Unit tests for {@link ShadowChoreographer}. */
@RunWith(AndroidJUnit4.class)
public class ShadowChoreographerTest {

  @Rule public final SetSystemPropertyRule setSystemPropertyRule = new SetSystemPropertyRule();

  @Test
  public void isValid() {
    ShadowPausedChoreographer shadowPausedChoreographer =
        Shadow.extract(Choreographer.getInstance());
    assertThat(shadowPausedChoreographer.isInitialized()).isTrue();
  }

  @Test
  public void setPaused_isPaused_doesntRun() {
    ShadowChoreographer.setPaused(true);
    long startTime = ShadowSystem.nanoTime();
    AtomicBoolean didRun = new AtomicBoolean();

    Choreographer.getInstance().postFrameCallback(frameTimeNanos -> didRun.set(true));
    ShadowLooper.idleMainLooper();

    assertThat(ShadowSystem.nanoTime()).isEqualTo(startTime);
    assertThat(didRun.get()).isFalse();
  }

  @Test
  public void setPaused_isPaused_doesntRunWhenClockAdancedLessThanFrameDelay() {
    ShadowChoreographer.setPaused(true);
    ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));
    AtomicBoolean didRun = new AtomicBoolean();

    Choreographer.getInstance().postFrameCallback(frameTimeNanos -> didRun.set(true));
    ShadowSystemClock.advanceBy(Duration.ofMillis(14));
    ShadowLooper.idleMainLooper();

    assertThat(didRun.get()).isFalse();
  }

  @Test
  public void setPaused_isPaused_runsWhenClockAdvanced() {
    ShadowChoreographer.setPaused(true);
    ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));
    long startTime = ShadowSystem.nanoTime();
    AtomicLong frameTimeNanos = new AtomicLong(-1);

    Choreographer.getInstance().postFrameCallback(frameTimeNanos::set);
    ShadowSystemClock.advanceBy(Duration.ofMillis(15));
    ShadowLooper.idleMainLooper();

    assertThat(frameTimeNanos.get()).isEqualTo(startTime + Duration.ofMillis(15).toNanos());
  }

  /** Verify Choreographer + SystemClock use nano precision */
  @Test
  public void setPaused_isPaused_runsWhenClockAdvanced_nanos() {
    ShadowChoreographer.setPaused(true);
    ShadowChoreographer.setFrameDelay(Duration.ofNanos(15_100_000));
    long startTime = ShadowSystem.nanoTime();
    AtomicLong frameTimeNanos = new AtomicLong(-1);

    Choreographer.getInstance().postFrameCallback(frameTimeNanos::set);
    ShadowSystemClock.advanceBy(Duration.ofMillis(15));
    ShadowLooper.idleMainLooper();

    assertThat(frameTimeNanos.get()).isEqualTo(-1);
    ShadowSystemClock.advanceBy(Duration.ofNanos(100_000));
    ShadowLooper.idleMainLooper();
    assertThat(frameTimeNanos.get()).isEqualTo(startTime + 15_100_000);
  }

  @Test
  public void setPaused_isNotPaused_advancesClockAndRuns() {
    ShadowChoreographer.setPaused(false);
    ShadowChoreographer.setFrameDelay(Duration.ofMillis(15));
    long startTime = ShadowSystem.nanoTime();
    AtomicBoolean didRun = new AtomicBoolean();

    Choreographer.getInstance().postFrameCallback(frameTimeNanos -> didRun.set(true));
    ShadowLooper.idleMainLooper();

    assertThat(ShadowSystem.nanoTime()).isEqualTo(startTime + Duration.ofMillis(15).toNanos());
    assertThat(didRun.get()).isTrue();
  }

  @Test
  public void setFrameDelay() {
    ShadowChoreographer.setPaused(false);
    ShadowChoreographer.setFrameDelay(Duration.ofMillis(30));
    long startTime = ShadowSystem.nanoTime();
    AtomicBoolean didRun = new AtomicBoolean();

    Choreographer.getInstance().postFrameCallback(frameTimeNanos -> didRun.set(true));
    ShadowLooper.idleMainLooper();

    assertThat(ShadowSystem.nanoTime()).isEqualTo(startTime + Duration.ofMillis(30).toNanos());
    assertThat(didRun.get()).isTrue();
  }

  @Test
  public void defaultFrameDelay() {
    setSystemPropertyRule.set("robolectric.defaultFrameDelayMs", "15");
    // Reset the shadow to pick up the new default frame delay.
    ShadowChoreographer.reset();
    ThingToAnimate thing = new ThingToAnimate();
    ObjectAnimator animator = ObjectAnimator.ofFloat(thing, "value", 0f, 1f);
    animator.setDuration(1000);
    animator.start();
    shadowOf(Looper.getMainLooper()).runToEndOfTasks();
    assertThat(thing.getSetCount()).isWithin(5).of(67); // Should be ~ (1000 / 15) ~ 67
  }

  public static class ThingToAnimate {
    private float value = 0f;
    private int setCount = 0;

    public void setValue(float value) {
      this.value = value;
      this.setCount++;
    }

    public float getValue() {
      return value;
    }

    public int getSetCount() {
      return setCount;
    }
  }
}
