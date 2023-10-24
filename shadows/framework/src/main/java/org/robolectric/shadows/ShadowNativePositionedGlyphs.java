package org.robolectric.shadows;

import android.graphics.text.MeasuredText;
import android.graphics.text.PositionedGlyphs;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.nativeruntime.DefaultNativeRuntimeLoader;
import org.robolectric.nativeruntime.PositionedGlyphsNatives;
import org.robolectric.shadows.ShadowNativePositionedGlyphs.Picker;
import org.robolectric.versioning.AndroidVersions.S;

/** Shadow for {@link PositionedGlyphs} that is backed by native code */
@Implements(value = PositionedGlyphs.class, minSdk = S.SDK_INT, shadowPicker = Picker.class)
public class ShadowNativePositionedGlyphs {
  @Implementation
  protected static int nGetGlyphCount(long minikinLayout) {
    return PositionedGlyphsNatives.nGetGlyphCount(minikinLayout);
  }

  @Implementation
  protected static float nGetTotalAdvance(long minikinLayout) {
    return PositionedGlyphsNatives.nGetTotalAdvance(minikinLayout);
  }

  @Implementation
  protected static float nGetAscent(long minikinLayout) {
    return PositionedGlyphsNatives.nGetAscent(minikinLayout);
  }

  @Implementation
  protected static float nGetDescent(long minikinLayout) {
    return PositionedGlyphsNatives.nGetDescent(minikinLayout);
  }

  @Implementation
  protected static int nGetGlyphId(long minikinLayout, int i) {
    return PositionedGlyphsNatives.nGetGlyphId(minikinLayout, i);
  }

  @Implementation
  protected static float nGetX(long minikinLayout, int i) {
    return PositionedGlyphsNatives.nGetX(minikinLayout, i);
  }

  @Implementation
  protected static float nGetY(long minikinLayout, int i) {
    return PositionedGlyphsNatives.nGetY(minikinLayout, i);
  }

  @Implementation
  protected static long nGetFont(long minikinLayout, int i) {
    return PositionedGlyphsNatives.nGetFont(minikinLayout, i);
  }

  @Implementation
  protected static long nReleaseFunc() {
    DefaultNativeRuntimeLoader.injectAndLoad();
    return PositionedGlyphsNatives.nReleaseFunc();
  }

  /** Shadow picker for {@link MeasuredText}. */
  public static final class Picker extends GraphicsShadowPicker<Object> {
    public Picker() {
      super(null, ShadowNativePositionedGlyphs.class);
    }
  }
}
