package org.robolectric.shadows;


import android.graphics.Matrix;
import java.util.List;
import java.util.Map;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowMatrix.Picker;
import org.robolectric.versioning.AndroidVersions.V;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(value = Matrix.class, shadowPicker = Picker.class)
public abstract class ShadowMatrix {
  public static final String TRANSLATE = "translate";
  public static final String SCALE = "scale";
  public static final String ROTATE = "rotate";
  public static final String SINCOS = "sincos";
  public static final String SKEW = "skew";
  public static final String MATRIX = "matrix";

  /**
   * A list of all 'pre' operations performed on this Matrix. The last operation performed will be
   * first in the list.
   *
   * @return A list of all 'pre' operations performed on this Matrix.
   */
  public abstract List<String> getPreOperations();

  /**
   * A list of all 'post' operations performed on this Matrix. The last operation performed will be
   * last in the list.
   *
   * @return A list of all 'post' operations performed on this Matrix.
   */
  public abstract List<String> getPostOperations();

  /**
   * A map of all 'set' operations performed on this Matrix.
   *
   * @return A map of all 'set' operations performed on this Matrix.
   */
  public abstract Map<String, String> getSetOperations();

  public abstract String getDescription();

  /** Shadow picker for {@link Matrix}. */
  public static final class Picker extends GraphicsShadowPicker<Object> {
    public Picker() {
      super(ShadowLegacyMatrix.class, ShadowNativeMatrix.class);
    }
  }

  @Implements(
      className = "android.graphics.Matrix$ExtraNatives",
      isInAndroidSdk = false,
      callNativeMethodsByDefault = true,
      shadowPicker = ShadowMatrix.ShadowExtraNatives.Picker.class,
      minSdk = V.SDK_INT)
  public static class ShadowExtraNatives {
    /** Shadow picker for {@link Matrix.ExtraNatives}. */
    public static final class Picker extends GraphicsShadowPicker<Object> {
      public Picker() {
        super(null, ShadowMatrix.ShadowExtraNatives.class);
      }
    }
  }
}
