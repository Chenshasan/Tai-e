package pascal.taie.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationElement;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.StringElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnnotationUtils {

    private static final Logger logger = LogManager.getLogger(AnnotationUtils.class);

    private static final String DEFAULT_KEY = "value";

    @Nullable
    public static String getStringElement(@Nullable Annotation anno) {
        return getStringElement(anno, DEFAULT_KEY);
    }

    @Nullable
    public static String getStringElement(@Nullable Annotation anno,
                                          String elementKey) {
        return Optional.ofNullable(anno)
                       .map(annotation -> annotation.getElement(elementKey))
                       .filter(ele -> ele instanceof StringElement)
                       .map(strEle -> ((StringElement) strEle).value())
                       .orElse(null);
    }

    @Nullable
    public static String getStringElementAlias(@Nullable Annotation anno,
                                               String elementKey1,
                                               String elementKey2) {
        return Optional.ofNullable(getStringElement(anno, elementKey1))
                       .orElseGet(() -> getStringElement(anno, elementKey2));
    }

    @Nonnull
    public static List<String> getStringArrayElement(@Nullable Annotation anno,
                                                     String elementKey) {
        return Optional.ofNullable(anno)
                       .map(annotation -> annotation.getElement(elementKey))
                       .filter(ele -> ele instanceof ArrayElement)
                       .map(ele -> ((ArrayElement) ele).elements())
                       .orElseGet(List::of)
                       .stream()
                       .filter(ele -> ele instanceof StringElement)
                       .map(ele -> ((StringElement) ele).value())
                       .collect(Collectors.toList());
    }

    @Nullable
    public static Annotation getAnnotationElement(@Nullable Annotation anno) {
        return getAnnotationElement(anno, DEFAULT_KEY);
    }

    @Nullable
    public static Annotation getAnnotationElement(@Nullable Annotation anno, String elementKey) {
        return Optional.ofNullable(anno)
                       .map(annotation -> annotation.getElement(elementKey))
                       .filter(ele -> ele instanceof AnnotationElement)
                       .map(strEle -> ((AnnotationElement) strEle).annotation())
                       .orElse(null);
    }

    @Nonnull
    public static List<Annotation> getAnnotationArrayElement(@Nullable Annotation anno,
                                                             String elementKey) {
        return Optional.ofNullable(anno)
                       .map(annotation -> annotation.getElement(elementKey))
                       .filter(ele -> ele instanceof ArrayElement)
                       .map(ele -> ((ArrayElement) ele).elements())
                       .orElseGet(List::of)
                       .stream()
                       .filter(ele -> ele instanceof AnnotationElement)
                       .map(ele -> ((AnnotationElement) ele).annotation())
                       .collect(Collectors.toList());
    }

    @Nonnull
    public static List<String> getStringArrayElementAlias(Annotation anno, String key1, String key2) {
        List<String> result1 = getStringArrayElement(anno, key1);
        List<String> result2 = getStringArrayElement(anno, key2);
        if ((result1.isEmpty() ^ result2.isEmpty()) || result1.equals(result2)) {
            return result1.isEmpty() ? result2 : result1;
        } else {
            logger.warn("Annotation '{}' has different content ('{}' and '{}') in alias key '{}' and '{}'",
                    anno, result1, result2, key1, key2);
        }
        return List.of();
    }
}
