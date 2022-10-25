package org.robolectric;
import com.example.objects.Dummy;
import com.example.objects.ParameterizedDummy;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.robolectric.annotation.processing.shadows.ShadowDummy;
import org.robolectric.annotation.processing.shadows.ShadowParameterizedDummy;
import org.robolectric.internal.ShadowProvider;
import org.robolectric.shadow.api.Shadow;

/**
 * Shadow mapper. Automatically generated by the Robolectric Annotation Processor.
 */
@Generated("org.robolectric.annotation.processing.RobolectricProcessor")
@SuppressWarnings({"unchecked","deprecation"})
public class Shadows implements ShadowProvider {
  private static final List<Map.Entry<String, String>> SHADOWS = new ArrayList<>(2);

  static {
    SHADOWS.add(new AbstractMap.SimpleImmutableEntry<>("com.example.objects.Dummy", "org.robolectric.annotation.processing.shadows.ShadowDummy"));
    SHADOWS.add(new AbstractMap.SimpleImmutableEntry<>("com.example.objects.ParameterizedDummy", "org.robolectric.annotation.processing.shadows.ShadowParameterizedDummy"));
  }

  public static ShadowDummy shadowOf(Dummy actual) {
    return (ShadowDummy) Shadow.extract(actual);
  }

  public static <T,N extends Number> ShadowParameterizedDummy<T,N> shadowOf(ParameterizedDummy<T,N> actual) {
    return (ShadowParameterizedDummy<T,N>) Shadow.extract(actual);
  }

  @Override
  public void reset() {
    ShadowDummy.resetter_method();
  }

  @Override
  public Collection<Map.Entry<String, String>> getShadows() {
    return SHADOWS;
  }

  @Override
  public String[] getProvidedPackageNames() {
    return new String[] {
        "com.example.objects"
    };
  }

}