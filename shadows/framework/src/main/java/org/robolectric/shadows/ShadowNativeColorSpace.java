package org.robolectric.shadows;

import android.graphics.ColorSpace;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowNativeColorSpace.Picker;
import org.robolectric.versioning.AndroidVersions.V;

/** Shadow for {@link ColorSpace} that defers its static initializer. */
@Implements(
    value = ColorSpace.class,
    minSdk = V.SDK_INT,
    isInAndroidSdk = false,
    shadowPicker = Picker.class)
public class ShadowNativeColorSpace {

  /** Shadow picker for {@link ColorSpace}. */
  public static final class Picker extends GraphicsShadowPicker<Object> {
    public Picker() {
      super(null, ShadowNativeColorSpace.class);
    }
  }

  /** Shadow for {@link ColorSpace$Rgb$Native} that contains native functions. */
  @Implements(
      className = "android.graphics.ColorSpace$Rgb$Native",
      isInAndroidSdk = false,
      callNativeMethodsByDefault = true,
      shadowPicker = ShadowNativeColorSpace.ShadowNative.Picker.class,
      minSdk = V.SDK_INT)
  public static class ShadowNative {
    /** Shadow picker for {@link ColorSpace.Native}. */
    public static final class Picker extends GraphicsShadowPicker<Object> {
      public Picker() {
        super(null, ShadowNativeColorSpace.ShadowNative.class);
      }
    }
  }
}

