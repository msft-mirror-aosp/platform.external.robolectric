package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.O_MR1;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static org.robolectric.util.reflector.Reflector.reflector;

import android.graphics.FontFamily;
import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.text.FontConfig;
import android.util.ArrayMap;
import android.util.Log;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.ClassName;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.InDevelopment;
import org.robolectric.nativeruntime.DefaultNativeRuntimeLoader;
import org.robolectric.nativeruntime.TypefaceNatives;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.reflector.Direct;
import org.robolectric.util.reflector.ForType;
import org.robolectric.util.reflector.Static;
import org.robolectric.versioning.AndroidVersions;
import org.robolectric.versioning.AndroidVersions.U;

/** Shadow for {@link Typeface} that is backed by native code */
@Implements(
    value = Typeface.class,
    minSdk = O,
    isInAndroidSdk = false,
    callNativeMethodsByDefault = true)
public class ShadowNativeTypeface extends ShadowTypeface {

  private static final String TAG = "ShadowNativeTypeface";

  // Style value for building typeface.
  private static final int STYLE_NORMAL = 0;
  private static final int STYLE_ITALIC = 1;

  @Implementation(minSdk = S)
  protected static void __staticInitializer__() {
    if (RuntimeEnvironment.getApiLevel() <= U.SDK_INT) {
      Shadow.directInitialize(Typeface.class);
      // Initialize the system font map. In real Android this is done as part of Application startup
      // and uses a more complex SharedMemory system not supported in Robolectric.
      Typeface.loadPreinstalledSystemFontMap();
    }
    // The Typeface static initializer invokes its own native methods. This has to be deferred
    // starting in Android V.
  }

  @Implementation(minSdk = P, maxSdk = P)
  protected static void buildSystemFallback(
      String xmlPath,
      String systemFontDir,
      ArrayMap<String, Typeface> fontMap,
      ArrayMap<String, FontFamily[]> fallbackMap) {
    String fontDir = System.getProperty("robolectric.nativeruntime.fontdir");
    Preconditions.checkNotNull(fontDir);
    Preconditions.checkState(new File(fontDir).isDirectory(), "Missing fonts directory");
    Preconditions.checkState(
        fontDir.endsWith(File.separator), "Fonts directory must end with a slash");
    reflector(TypefaceReflector.class)
        .buildSystemFallback(fontDir + "fonts.xml", fontDir, fontMap, fallbackMap);
  }

  @Implementation(minSdk = O, maxSdk = O_MR1)
  protected static File getSystemFontConfigLocation() {
    // Ensure that the Robolectric native runtime is loaded in ordere to ensure that the
    // `robolectric.nativeruntime.fontdir` system property is valid.
    DefaultNativeRuntimeLoader.injectAndLoad();
    String fontDir = System.getProperty("robolectric.nativeruntime.fontdir");
    Preconditions.checkNotNull(fontDir);
    Preconditions.checkState(new File(fontDir).isDirectory(), "Missing fonts directory");
    Preconditions.checkState(
        fontDir.endsWith(File.separator), "Fonts directory must end with a slash");
    return new File(fontDir);
  }

  @SuppressWarnings("unchecked")
  @Implementation(minSdk = O, maxSdk = O_MR1)
  protected static @ClassName("android.graphics.FontFamily") Object makeFamilyFromParsed(
      @ClassName("android.text.FontConfig$Family") Object family,
      Map<String, ByteBuffer> bufferForPath) {
    FontConfigFamilyReflector reflector = reflector(FontConfigFamilyReflector.class, family);

    FontFamily fontFamily =
        Shadow.newInstance(
            FontFamily.class,
            new Class<?>[] {String.class, int.class},
            new Object[] {reflector.getLanguage(), reflector.getVariant()});
    for (FontConfig.Font font : reflector.getFonts()) {
      String fullPathName =
          System.getProperty("robolectric.nativeruntime.fontdir")
              + reflector(FontConfigFontReflector.class, font).getFontName();
      ByteBuffer fontBuffer = bufferForPath.get(fullPathName);
      if (fontBuffer == null) {
        try (FileInputStream file = new FileInputStream(fullPathName)) {
          FileChannel fileChannel = file.getChannel();
          long fontSize = fileChannel.size();
          fontBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fontSize);
          bufferForPath.put(fullPathName, fontBuffer);
        } catch (IOException e) {
          Log.w(TAG, "Error mapping font file " + fullPathName);
          continue;
        }
      }
      if (!fontFamily.addFontFromBuffer(
          fontBuffer,
          font.getTtcIndex(),
          font.getAxes(),
          font.getWeight(),
          font.isItalic() ? STYLE_ITALIC : STYLE_NORMAL)) {
        Log.e(TAG, "Error creating font " + fullPathName + "#" + font.getTtcIndex());
      }
    }
    if (!fontFamily.freeze()) {
      // Treat as system error since reaching here means that a system pre-installed font
      // can't be used by our font stack.
      Log.w(TAG, "Unable to load Family: " + reflector.getName() + ":" + reflector.getLanguage());
      return null;
    }
    return fontFamily;
  }

  @Implementation(maxSdk = U.SDK_INT)
  protected static long nativeCreateFromTypeface(long nativeInstance, int style) {
    return TypefaceNatives.nativeCreateFromTypeface(nativeInstance, style);
  }

  @Implementation(minSdk = O, maxSdk = U.SDK_INT)
  protected static long nativeCreateFromTypefaceWithExactStyle(
      long nativeInstance, int weight, boolean italic) {
    return TypefaceNatives.nativeCreateFromTypefaceWithExactStyle(nativeInstance, weight, italic);
  }

  @Implementation(minSdk = O, maxSdk = U.SDK_INT)
  protected static long nativeCreateFromTypefaceWithVariation(
      long nativeInstance, List<FontVariationAxis> axes) {
    return TypefaceNatives.nativeCreateFromTypefaceWithVariation(nativeInstance, axes);
  }

  @Implementation(maxSdk = U.SDK_INT)
  protected static long nativeCreateWeightAlias(long nativeInstance, int weight) {
    return TypefaceNatives.nativeCreateWeightAlias(nativeInstance, weight);
  }

  @Implementation(minSdk = O, maxSdk = R)
  protected static long nativeCreateFromArray(long[] familyArray, int weight, int italic) {
    return TypefaceNatives.nativeCreateFromArray(familyArray, 0, weight, italic);
  }

  @Implementation(minSdk = S, maxSdk = U.SDK_INT)
  protected static long nativeCreateFromArray(
      long[] familyArray, long fallbackTypeface, int weight, int italic) {
    return TypefaceNatives.nativeCreateFromArray(familyArray, fallbackTypeface, weight, italic);
  }

  @Implementation(minSdk = O, maxSdk = U.SDK_INT)
  protected static int[] nativeGetSupportedAxes(long nativeInstance) {
    return TypefaceNatives.nativeGetSupportedAxes(nativeInstance);
  }

  @Implementation(maxSdk = U.SDK_INT)
  protected static void nativeSetDefault(long nativePtr) {
    TypefaceNatives.nativeSetDefault(nativePtr);
  }

  @Implementation(maxSdk = U.SDK_INT)
  protected static int nativeGetStyle(long nativePtr) {
    return TypefaceNatives.nativeGetStyle(nativePtr);
  }

  @Implementation(minSdk = O, maxSdk = U.SDK_INT)
  protected static int nativeGetWeight(long nativePtr) {
    return TypefaceNatives.nativeGetWeight(nativePtr);
  }

  @Implementation(minSdk = P, maxSdk = U.SDK_INT)
  protected static long nativeGetReleaseFunc() {
    DefaultNativeRuntimeLoader.injectAndLoad();
    return TypefaceNatives.nativeGetReleaseFunc();
  }

  @Implementation(minSdk = S, maxSdk = TIRAMISU)
  protected static int nativeGetFamilySize(long nativePtr) {
    return TypefaceNatives.nativeGetFamilySize(nativePtr);
  }

  @Implementation(minSdk = S, maxSdk = TIRAMISU)
  protected static long nativeGetFamily(long nativePtr, int index) {
    return TypefaceNatives.nativeGetFamily(nativePtr, index);
  }

  @Implementation(minSdk = AndroidVersions.Baklava.SDK_INT)
  @InDevelopment
  protected static boolean nativeIsVariationInstance(long nativePtr) {
    return false;
    // TODO: call the real impl when it's finally available in native binaries
    // return TypefaceNatives.nativeIsVariationInstance(nativePtr);
  }

  @Implementation(minSdk = Q, maxSdk = U.SDK_INT)
  protected static void nativeRegisterGenericFamily(String str, long nativePtr) {
    TypefaceNatives.nativeRegisterGenericFamily(str, nativePtr);
  }

  @Implementation(minSdk = S, maxSdk = TIRAMISU)
  protected static int nativeWriteTypefaces(ByteBuffer buffer, long[] nativePtrs) {
    return TypefaceNatives.nativeWriteTypefaces(buffer, nativePtrs);
  }

  @Implementation(minSdk = U.SDK_INT, maxSdk = U.SDK_INT)
  protected static int nativeWriteTypefaces(ByteBuffer buffer, int position, long[] nativePtrs) {
    return nativeWriteTypefaces(buffer, nativePtrs);
  }

  @Implementation(minSdk = S, maxSdk = TIRAMISU)
  protected static long[] nativeReadTypefaces(ByteBuffer buffer) {
    return TypefaceNatives.nativeReadTypefaces(buffer);
  }

  @Implementation(minSdk = U.SDK_INT, maxSdk = U.SDK_INT)
  protected static long[] nativeReadTypefaces(ByteBuffer buffer, int position) {
    return nativeReadTypefaces(buffer);
  }

  @Implementation(minSdk = S, maxSdk = U.SDK_INT)
  protected static void nativeForceSetStaticFinalField(String fieldName, Typeface typeface) {
    TypefaceNatives.nativeForceSetStaticFinalField(fieldName, typeface);
  }

  @Implementation(minSdk = S, maxSdk = U.SDK_INT)
  protected static void nativeAddFontCollections(long nativePtr) {
    TypefaceNatives.nativeAddFontCollections(nativePtr);
  }

  @Implementation(minSdk = U.SDK_INT, maxSdk = U.SDK_INT)
  protected static void nativeRegisterLocaleList(String locales) {
    // no-op
  }

  static void ensureInitialized() {
    try {
      // Forces static initialization. This should be called before any native code that calls
      // Typeface::resolveDefault.
      Class.forName("android.graphics.Typeface");
    } catch (ClassNotFoundException e) {
      throw new LinkageError("Unable to load Typeface", e);
    }
  }

  @Override
  public FontDesc getFontDescription() {
    throw new UnsupportedOperationException(
        "Legacy ShadowTypeface description APIs are not supported");
  }

  /**
   * Shadow for {@link Typeface.Builder}. It is empty to avoid using the legacy {@link
   * Typeface.Builder} shadow.
   */
  @Implements(
      value = Typeface.Builder.class,
      minSdk = P,
      shadowPicker = ShadowNativeTypefaceBuilder.Picker.class,
      isInAndroidSdk = false)
  public static class ShadowNativeTypefaceBuilder {
    /** Shadow picker for {@link Typeface.Builder}. */
    public static final class Picker extends GraphicsShadowPicker<Object> {
      public Picker() {
        super(ShadowLegacyTypeface.ShadowBuilder.class, ShadowNativeTypefaceBuilder.class);
      }
    }
  }

  @ForType(Typeface.class)
  interface TypefaceReflector {
    @CanIgnoreReturnValue
    @Static
    @Direct
    FontConfig.Alias[] buildSystemFallback(
        String xmlPath,
        String fontDir,
        ArrayMap<String, Typeface> fontMap,
        ArrayMap<String, FontFamily[]> fallbackMap);
  }

  @ForType(className = "android.text.FontConfig$Family")
  interface FontConfigFamilyReflector {
    String getLanguage();

    int getVariant();

    FontConfig.Font[] getFonts();

    String getName();
  }

  @ForType(className = "android.text.FontConfig$Font")
  interface FontConfigFontReflector {
    String getFontName();
  }
}
