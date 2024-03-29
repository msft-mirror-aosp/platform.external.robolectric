package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.O;

import android.graphics.PathEffect;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.nativeruntime.PathEffectNatives;
import org.robolectric.shadows.ShadowNativePathEffect.Picker;
import org.robolectric.versioning.AndroidVersions.U;

/** Shadow for {@link PathEffect} that is backed by native code */
@Implements(
    value = PathEffect.class,
    minSdk = O,
    shadowPicker = Picker.class,
    callNativeMethodsByDefault = true)
public class ShadowNativePathEffect {

  @Implementation(minSdk = O, maxSdk = U.SDK_INT)
  protected static void nativeDestructor(long nativePatheffect) {
    PathEffectNatives.nativeDestructor(nativePatheffect);
  }

  /** Shadow picker for {@link PathEffect}. */
  public static final class Picker extends GraphicsShadowPicker<Object> {
    public Picker() {
      super(null, ShadowNativePathEffect.class);
    }
  }
}
