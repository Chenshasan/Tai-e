package pascal.taie.analysis.pta.plugin.spring;

import pascal.taie.World;
import pascal.taie.analysis.pta.plugin.spring.enums.AnnotationEnum;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A manager extracting the annotation-related information from all classes in
 * the {@link World}
 */
public class AnnotationManager {

    private final Collection<String> classes;

    /**
     * Map[AnnotationType on class] = (JClass, Annotation on class)s
     */
    private final MultiMap<String, Pair<JClass, Annotation>> anno2Classes =
            Maps.newMultiMap();

    /**
     * Map[AnnotationType on field] = (JField, Annotation on field)s
     */
    private final MultiMap<String, Pair<JField, Annotation>> anno2Fields =
            Maps.newMultiMap();

    /**
     * Map[AnnotationType on method] = (JMethod, Annotation on method)s
     */
    private final MultiMap<String, Pair<JMethod, Annotation>> anno2Methods =
            Maps.newMultiMap();

    /**
     * Map[AnnotationType on class, AnnotationType on method] = (JMethod, Annotation on method)s
     */
    private final MultiMap<Pair<String, String>, Pair<JMethod, Annotation>> classAndMethodAnno2Methods =
            Maps.newMultiMap();

    /**
     * the annotations this manager focus on
     */
    private final Collection<String> concernedAnnotations;

    public AnnotationManager(Collection<String> classes) {
        this.classes = classes;
        this.concernedAnnotations = Sets.newSet();
    }

    public void addAnnotations(String... annoTypes) {
        this.concernedAnnotations.addAll(Arrays.asList(annoTypes));
    }

    public void addAnnotations(AnnotationEnum... annotations) {
        for (AnnotationEnum annotationEnum : annotations) {
            this.concernedAnnotations.add(annotationEnum.getName());
        }
    }

    public void initialize() {
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        classes.stream()
               .map(classHierarchy::getClass)
               .filter(Objects::nonNull)
               .forEach(this::visitClass);
    }

    private void visitClass(JClass jClass) {
        for (Annotation anno : jClass.getAnnotations()) {
            if (concernedAnnotations.contains(anno.getType())) {
                anno2Classes.put(anno.getType(), new Pair<>(jClass, anno));
            }
        }
        for (JField jField : jClass.getDeclaredFields()) {
            for (Annotation anno : jField.getAnnotations()) {
                if (concernedAnnotations.contains(anno.getType())) {
                    anno2Fields.put(anno.getType(), new Pair<>(jField, anno));
                }
            }
        }
        for (JMethod jMethod : jClass.getDeclaredMethods()) {
            for (Annotation anno : jMethod.getAnnotations()) {
                if (concernedAnnotations.contains(anno.getType())) {
                    anno2Methods.put(anno.getType(), new Pair<>(jMethod, anno));
                }
            }
        }
        for (Annotation classAnno : jClass.getAnnotations()) {
            if (concernedAnnotations.contains(classAnno.getType())) {
                for (JMethod jMethod : jClass.getDeclaredMethods()) {
                    for (Annotation methodAnno : jMethod.getAnnotations()) {
                        if (concernedAnnotations.contains(methodAnno.getType())) {
                            classAndMethodAnno2Methods.put(
                                new Pair<>(classAnno.getType(), methodAnno.getType()),
                                new Pair<>(jMethod, methodAnno)
                            );
                        }
                    }
                }
            }
        }
    }

    public void visitClasses(AnnotationEnum annotation,
                             BiConsumer<JClass, Annotation> consumer) {
        anno2Classes.get(annotation.getName())
                    .forEach(pair -> consumer.accept(pair.first(), pair.second()));
    }

    public void visitFields(AnnotationEnum annotation,
                            BiConsumer<JField, Annotation> consumer) {
        anno2Fields.get(annotation.getName())
                   .forEach(pair -> consumer.accept(pair.first(), pair.second()));
    }

    public void visitMethods(AnnotationEnum annotation,
                             BiConsumer<JMethod, Annotation> consumer) {
        anno2Methods.get(annotation.getName())
                    .forEach(pair -> consumer.accept(pair.first(), pair.second()));
    }

    public void visitMethodsInClasses(AnnotationEnum classAnnotation,
                                      AnnotationEnum methodAnnotation,
                                      BiConsumer<JMethod, Annotation> consumer) {
        classAndMethodAnno2Methods.get(new Pair<>(classAnnotation.getName(), methodAnnotation.getName()))
                                  .forEach(pair -> consumer.accept(pair.first(), pair.second()));
    }

    public Stream<JField> getFields(AnnotationEnum annotation) {
        return anno2Fields.get(annotation.getName())
                          .stream()
                          .map(Pair::first);
    }

    public Collection<JMethod> getMethodsInClasses(AnnotationEnum classAnnotation,
                                               AnnotationEnum methodAnnotation) {
        return classAndMethodAnno2Methods.get(new Pair<>(classAnnotation.getName(), methodAnnotation.getName()))
                                         .stream()
                                         .map(Pair::first)
                                         .toList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AnnotationManager@").append(Objects.hashCode(this)).append("\n");
        sb.append("class<->annotation: ").append("\n");
        sb.append(anno2Classes.values()
                              .stream()
                              .map(Pair::toString)
                              .collect(Collectors.joining(", ", "[", "]")));
        sb.append("\n");
        sb.append("field<->annotation: ").append("\n");
        sb.append(anno2Fields.values()
                             .stream()
                             .map(Pair::toString)
                             .collect(Collectors.joining(", ", "[", "]")));
        sb.append("\n");
        sb.append("method<->annotation: ").append("\n");
        sb.append(anno2Methods.values()
                              .stream()
                              .map(Pair::toString)
                              .collect(Collectors.joining(", ", "[", "]")));
        return sb.toString();
    }
}
