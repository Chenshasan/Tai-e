package pascal.taie.analysis.pta.plugin.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.plugin.spring.enums.SpringAnnotation;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnnotationUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SpringWebAnalysis implements MicroserviceAware {

    private static final Logger logger = LogManager.getLogger(SpringWebAnalysis.class);

    private MicroserviceHolder holder;

    private AnnotationManager annotationManager;

    private final Collection<RequestMappingEntry> requestMappingEntries = Sets.newSet();

    private final MultiMap<JClass, RequestMappingEntry> jClass2RequestMappingEntry = Maps.newMultiMap();

    private final Set<JClass> handledControllerClasses = Sets.newSet();

    @Override
    public void setMicroserviceHolder(MicroserviceHolder holder) {
        this.holder = holder;
        this.annotationManager = holder.getAnnotationManager();

        annotationManager.addAnnotations(
            SpringAnnotation.CONTROLLER,
            SpringAnnotation.REST_CONTROLLER,
            SpringAnnotation.REQUEST_MAPPING,
            SpringAnnotation.GET_MAPPING,
            SpringAnnotation.POST_MAPPING,
            SpringAnnotation.PATCH_MAPPING,
            SpringAnnotation.DELETE_MAPPING,
            SpringAnnotation.PUT_MAPPING
        );
    }

    public void initialize() {
        annotationManager.visitClasses(SpringAnnotation.CONTROLLER, this::acceptControllerClass);
        annotationManager.visitClasses(SpringAnnotation.REST_CONTROLLER, this::acceptControllerClass);
        annotationManager.visitMethods(SpringAnnotation.REQUEST_MAPPING, this::acceptMethod); // should be first
        annotationManager.visitMethods(SpringAnnotation.GET_MAPPING, this::acceptMethod);
        annotationManager.visitMethods(SpringAnnotation.POST_MAPPING, this::acceptMethod);
        annotationManager.visitMethods(SpringAnnotation.PATCH_MAPPING, this::acceptMethod);
        annotationManager.visitMethods(SpringAnnotation.DELETE_MAPPING, this::acceptMethod);
        annotationManager.visitMethods(SpringAnnotation.PUT_MAPPING, this::acceptMethod);
    }

    public void logEndpoints() {
        if (!requestMappingEntries.isEmpty()) {
            List<RequestMappingEntry> sortedEndpoints = requestMappingEntries
                    .stream()
                    .sorted((o1, o2) -> {
                        JClass c1 = o1.getMethod().getDeclaringClass();
                        JClass c2 = o2.getMethod().getDeclaringClass();
                        if (!c1.equals(c2)) {
                            return c1.getName().compareTo(c2.getName());
                        }
                        return o1.getUrl().compareTo(o2.getUrl());
                    }).toList();

            logger.info("'{}' found {} web endpoints as follows: \n{}",
                    holder.getServiceName(),
                    sortedEndpoints.size(),
                    sortedEndpoints.stream()
                                   .map(Object::toString)
                                   .collect(Collectors.joining("\n\t", "\t", ""))
            );
//            logger.info("'{}' found {} web endpoints methods as follows: \n{}",
//                    holder.getServiceName(),
//                    sortedEndpoints.size(),
//                    sortedEndpoints.stream()
//                                   .map(RequestMappingEntry::getMethod)
//                                   .map(Objects::toString)
//                                   .collect(Collectors.joining("\",\n\t\"", "\t\"", "\","))
//            );
        }
    }

    private void acceptControllerClass(JClass jClass, Annotation annotation) {
        for (JMethod method : jClass.getDeclaredMethods()) {
            Annotation mappingAnno = getMappingAnnotation(jClass, method);
            if (mappingAnno != null) {
                acceptMethod(method, mappingAnno);
            }
        }
        handledControllerClasses.add(jClass);
    }

    @Nullable
    private Annotation getMappingAnnotation(JClass jClass, JMethod method) {
        Annotation mappingAnno = getMappingAnnotation(method);
        if (mappingAnno != null) {
            return mappingAnno;
        }
        for (JClass anInterface : jClass.getInterfaces()) {
            JMethod interfaceMethod = anInterface.getDeclaredMethod(method.getSubsignature());
            if (interfaceMethod != null) {
                mappingAnno = getMappingAnnotation(interfaceMethod);
                if (mappingAnno != null) {
                    return mappingAnno;
                }
            }
        }
        return null;
    }

    @Nullable
    private Annotation getMappingAnnotation(JMethod method) {
        final SpringAnnotation[] springAnnotations = {
            SpringAnnotation.REQUEST_MAPPING,
            SpringAnnotation.GET_MAPPING,
            SpringAnnotation.POST_MAPPING,
            SpringAnnotation.PATCH_MAPPING,
            SpringAnnotation.DELETE_MAPPING,
            SpringAnnotation.PUT_MAPPING,
        };

        for (SpringAnnotation springAnnotation : springAnnotations) {
            Annotation mappingAnno = method.getAnnotation(springAnnotation.getName());
            if (mappingAnno != null) {
                return mappingAnno;
            }
        }
        return null;
    }

    private void acceptMethod(JMethod jMethod, Annotation annotation) {
        JClass jClass = jMethod.getDeclaringClass();

        if (handledControllerClasses.contains(jClass)) {
            return;
        }

        boolean inControllerClass = isControllerClass(jMethod.getDeclaringClass());
        for (String url1 : getRequestMappingUrlOnClass(jClass)) {
            List<String> url2s = Optional.of(AnnotationUtils.getStringArrayElementAlias(
                                             annotation, "value", "path"))
                                         .filter(Predicate.not(List::isEmpty))
                                         .orElse(List.of(""));
            for (String url2 : url2s) {
                String path = RequestMappingEntry.mergeUrl(url1, url2);
                for (SpringAnnotation mappingType : getRequestMappingMethods(annotation)) {
                    RequestMappingEntry requestMappingEntry = new RequestMappingEntry(
                            annotation, mappingType, path, jMethod);
                    jClass2RequestMappingEntry.put(jClass, requestMappingEntry);
                    if (inControllerClass) {
                        requestMappingEntries.add(requestMappingEntry);
                    }
                }
            }
        }

        // TODO: filter the real request mapping method that are not in controller class (excluding Feign Method)
        if (!inControllerClass && !jClass.isInterface()) {
            logger.warn("JMethod '{}' with Annotation '{}' is not in Class with @Controller or @RestController",
                    jMethod, annotation);
        }
    }

    private List<SpringAnnotation> getRequestMappingMethods(Annotation annotation) {
        List<SpringAnnotation> result = new ArrayList<>();
        SpringAnnotation springAnnotation = SpringAnnotation.of(annotation.getType());
        if (SpringAnnotation.REQUEST_MAPPING == springAnnotation) {
            if (annotation.getElement("method") instanceof ArrayElement arrayElement) {
                for (Element element : arrayElement.elements()) {
                    if (element instanceof EnumElement enumElement) {
                        switch (enumElement.name()) {
                            case "GET" -> result.add(SpringAnnotation.GET_MAPPING);
                            case "POST" -> result.add(SpringAnnotation.POST_MAPPING);
                            case "PUT" -> result.add(SpringAnnotation.PUT_MAPPING);
                            case "DELETE" -> result.add(SpringAnnotation.DELETE_MAPPING);
                            case "PATCH" -> result.add(SpringAnnotation.PATCH_MAPPING);
                        }
                    }
                }
            }
        } else {
            result.add(springAnnotation);
        }
        if (result.isEmpty()) {
            result.add(SpringAnnotation.REQUEST_MAPPING);
        }
        return result;
    }

    private boolean isControllerClass(JClass jClass) {
        return Objects.nonNull(jClass.getAnnotation(SpringAnnotation.CONTROLLER.getName()))
            || Objects.nonNull(jClass.getAnnotation(SpringAnnotation.REST_CONTROLLER.getName()));
    }

    private List<String> getRequestMappingUrlOnClass(JClass jClass) {
        return Optional
            .ofNullable(getRequestMappingOnClass(jClass))
            .map(anno -> AnnotationUtils.getStringArrayElementAlias(anno, "value", "path"))
            .orElseGet(() -> Collections.singletonList(""));
    }

    @Nullable
    private Annotation getRequestMappingOnClass(JClass jClass) {
        Annotation annotation = jClass.getAnnotation(SpringAnnotation.REQUEST_MAPPING.getName());
        if (annotation != null) {
            return annotation;
        }
        for (JClass anInterface : jClass.getInterfaces()) {
            annotation = anInterface.getAnnotation(SpringAnnotation.REQUEST_MAPPING.getName());
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * merge two url
     */


    public Collection<RequestMappingEntry> getRequestMappingEntries() {
        return requestMappingEntries;
    }

    @Nullable
    public Collection<RequestMappingEntry> getRequestMappingEntries(JClass jClass) {
        Collection<RequestMappingEntry> result = Sets.newSet();
        jClass.getInterfaces()
              .stream()
              .map(jClass2RequestMappingEntry::get)
              .forEach(result::addAll);
        for (JClass tempClz = jClass;
             !(tempClz == null || ClassNames.OBJECT.equals(tempClz.getName()));
             tempClz = tempClz.getSuperClass()) {
            result.addAll(jClass2RequestMappingEntry.get(tempClz));
        }
        return result;
    }
}
