package pascal.taie.analysis.pta.plugin.spring;

import pascal.taie.analysis.pta.plugin.spring.enums.SpringAnnotation;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.JClassUtils;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.function.Predicate;

public class SpringEntryPointManager implements MicroserviceAware {

    private final Set<JMethod> entrypoint = Sets.newSet();

    private MicroserviceHolder holder;

    private AnnotationManager annotationManager;

    private ClassHierarchy classHierarchy;

    private Set<String> appClasses;

    @Override
    public void setMicroserviceHolder(MicroserviceHolder holder) {
        this.holder = holder;
        this.annotationManager = holder.getAnnotationManager();
        this.classHierarchy = holder.getClassHierarchy();
        this.appClasses = holder.getAppClasses();

        this.annotationManager.addAnnotations(
                SpringAnnotation.CONTROLLER,
                SpringAnnotation.REST_CONTROLLER,
                SpringAnnotation.REQUEST_MAPPING,
                SpringAnnotation.CONTROLLER_ADVICE,
                SpringAnnotation.CONFIGURATION
        );
    }

    public void initialize() {
        annotationManager.visitClasses(SpringAnnotation.CONTROLLER, this::acceptEntrypointClass);
        annotationManager.visitClasses(SpringAnnotation.REST_CONTROLLER, this::acceptEntrypointClass);
        annotationManager.visitClasses(SpringAnnotation.REQUEST_MAPPING, this::acceptEntrypointClass);
        annotationManager.visitClasses(SpringAnnotation.CONTROLLER_ADVICE, this::acceptEntrypointClass);
        annotationManager.visitClasses(SpringAnnotation.CONFIGURATION, this::acceptEntrypointClass);

        JClass filterClass = classHierarchy.getClass("javax.servlet.Filter");
        if (filterClass != null) {
            classHierarchy.getAllSubclassesOf(filterClass)
                          .stream()
                          .filter(JClass::isApplication)
                          .filter(Predicate.not(JClass::isAbstract))
                          .filter(Predicate.not(JClass::isInterface))
                          .filter(JClassUtils::isApplicationInMicroservices)
                          .forEach(this::acceptEntrypointClass);
        }

        // see https://bitbucket.org/yanniss/doop/src/46b1d002eb90625d5e330112144ec7edf789f5b3/souffle-logic/addons/open-programs/rules-jackee.dl#lines-327:330
        JClass ibClass = classHierarchy.getClass("org.springframework.beans.factory.InitializingBean");
        if (ibClass != null) {
            classHierarchy.getAllSubclassesOf(ibClass)
                          .stream()
                          .filter(c -> {
                              JMethod m = c.getDeclaredMethod("afterPropertiesSet");
                              return m != null && !m.isAbstract();
                          })
                          .forEach(this::acceptEntrypointClass);
        }
    }

    private void acceptEntrypointClass(JClass jClass, Annotation annotation) {
        if (!jClass.isApplication()
                || jClass.isAbstract()
                || jClass.isInterface()
                || !JClassUtils.isApplicationInMicroservices(jClass)) {
            return;
        }
        this.acceptEntrypointClass(jClass);
    }

    private void acceptEntrypointClass(JClass jClass) {
        jClass.getDeclaredMethods()
              .stream()
              .filter(Predicate.not(JMethod::isAbstract))
              .filter(Predicate.not(JMethod::isNative))
              .forEach(entrypoint::add);
    }

    public Set<JMethod> getEntrypoint() {
        return entrypoint;
    }
}
