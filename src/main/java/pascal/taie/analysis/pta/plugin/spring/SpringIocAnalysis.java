package pascal.taie.analysis.pta.plugin.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.spring.enums.JavaxAnnotation;
import pascal.taie.analysis.pta.plugin.spring.enums.SpringAnnotation;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.AnnotationUtils;
import pascal.taie.util.JClassUtils;
import pascal.taie.util.XmlConfiguration;
import pascal.taie.util.collection.Sets;

import java.util.*;

/**
 * Spring Inversion of Control (a.k.a. Dependency Injection) analysis.
 *
 * Analyzes xml files and annotations to create Spring Beans <br>
 * Scans and injects Spring Beans into fields and parameters <br>
 *
 */
public class SpringIocAnalysis implements MicroserviceAware {

    private static final Logger logger = LogManager.getLogger(SpringIocAnalysis.class);

    private static final Descriptor BEAN_DESC = () -> "SpringBeanObj";


    private MicroserviceHolder microserviceHolder;

    private AnnotationManager annotationManager;

    private XmlConfiguration xmlConfiguration;

    private BeanManager beanManager;

    private Solver solver;

    private ClassHierarchy classHierarchy;

    /**
     * used to create beans whether the bean class is annotated or not
     */
    private final List<String> activatedBeanClasses = new ArrayList<>();

    public void addActivatedBeanClasses(String... beanClasses) {
        Collections.addAll(this.activatedBeanClasses, beanClasses);
    }

    /**
     * Autowired methods, Bean methods, Bean constructors
     */
    private final Collection<JMethod> injectedMethods = Sets.newSet();

    @Override
    public void setMicroserviceHolder(MicroserviceHolder holder) {
        this.microserviceHolder = holder;
        this.annotationManager = holder.getAnnotationManager();
        this.xmlConfiguration = holder.getXmlConfiguration();
        this.beanManager = holder.getBeanManager();
        this.solver = holder.getSolver();
        this.classHierarchy = World.get().getClassHierarchy();

        annotationManager.addAnnotations(
            SpringAnnotation.COMPONENT,
            SpringAnnotation.REST_CONTROLLER,
            SpringAnnotation.CONTROLLER,
            SpringAnnotation.SERVICE,
            SpringAnnotation.REPOSITORY,
            SpringAnnotation.CONFIGURATION,
            SpringAnnotation.CONFIGURATION_PROPERTIES,

            SpringAnnotation.SPRING_BOOT_APPLICATION,
            SpringAnnotation.SPRING_BOOT_CONFIGURATION,

            SpringAnnotation.BEAN,

            SpringAnnotation.AUTOWIRED,
            SpringAnnotation.SCOPE,          // TODO: model this Spring Annotation
            SpringAnnotation.COMPONENT_SCAN, // TODO: model this Spring Annotation
            SpringAnnotation.QUALIFIER,

            JavaxAnnotation.RESOURCE
        );

        xmlConfiguration.addConcernedKeys("bean");

        // TODO: move it to a suitable class
        activatedBeanClasses.add("org.springframework.data.redis.core.RedisTemplate");
        activatedBeanClasses.add("org.springframework.jdbc.core.JdbcTemplate");
    }

    public void createBeans() {
        // visit bean classes in XML
        for (Node tag : xmlConfiguration.getNodesByTag("bean")) {
            NamedNodeMap attrs = tag.getAttributes();
            String clzName = attrs.getNamedItem("class").getTextContent();
            String beanName;
            // get beanName
            Node id = attrs.getNamedItem("id");
            if (id != null) {
                beanName = id.getTextContent();
            } else {
                beanName = JClassUtils.getLowerCamelSimpleName(clzName);
            }
            // create bean
            try {
                JClass clz = classHierarchy.getClass(clzName);
                if (clz == null) {
                    logger.error("Missing class [{}] when creating bean [{}]", clzName, beanName);
                } else {
                    visitBeanClass(clz, beanName);
                }
            } catch (Exception ignored) { // TODO: handle exception
            }
        }
        // visit annotated bean classes
        annotationManager.visitClasses(SpringAnnotation.COMPONENT, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.REST_CONTROLLER, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.CONTROLLER, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.SERVICE, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.REPOSITORY, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.CONFIGURATION, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.CONFIGURATION_PROPERTIES, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.SPRING_BOOT_APPLICATION, this::visitBeanClass);
        annotationManager.visitClasses(SpringAnnotation.SPRING_BOOT_CONFIGURATION, this::visitBeanClass);
        // visit @Bean methods in @Configuration classes
        annotationManager.visitMethodsInClasses(SpringAnnotation.CONFIGURATION,
            SpringAnnotation.BEAN, this::visitBeanMethod);
        annotationManager.visitMethodsInClasses(SpringAnnotation.SPRING_BOOT_APPLICATION,
                SpringAnnotation.BEAN, this::visitBeanMethod);
        annotationManager.visitMethodsInClasses(SpringAnnotation.SPRING_BOOT_CONFIGURATION,
                SpringAnnotation.BEAN, this::visitBeanMethod);
        // create custom beans
        for (String customBeanClass : activatedBeanClasses) {
            JClass clz = classHierarchy.getClass(customBeanClass);
            if (clz != null) {
                visitBeanClass(clz, (Annotation) null);
            }
        }
    }

    private void visitBeanClass(JClass jClass, Annotation annotation) {
        if (beanManager.exist(jClass)) {
            return;
        }
        if (jClass.isInterface()) {
            logger.warn("Create Bean Error: interface '{}'", jClass.getName());
            return;
        }
        String beanName = Optional.ofNullable(AnnotationUtils.getStringElement(annotation))
                                  .orElseGet(() -> JClassUtils.getLowerCamelSimpleName(jClass));
        visitBeanClass(jClass, beanName);
    }

    private void visitBeanClass(JClass jClass, String beanName) {
        // mock obj
        Obj beanObj = MockObjUtils.getMockObj(solver, BEAN_DESC,
                beanName, jClass.getType());
        // create bean
        Bean bean = beanManager.create(jClass,
            SpringIocAnalysis::autowiredConstructorFirstGetter, beanObj, beanName);
    }

    private void visitBeanMethod(JMethod method, Annotation annotation) {
        if (method.getReturnType() instanceof ClassType classType) {
            List<String> beanNames = AnnotationUtils.getStringArrayElementAlias(annotation, "name", "value");
            if (beanNames.isEmpty()) {
                beanNames = Collections.singletonList(method.getName());
            }
            List<Var> inVars = method.getIR().getReturnVars();
            beanManager.createWithBeanNames(classType.getJClass(), beanNames, inVars);
        }
    }

    /**
     * TODO: inject to super's fields
     */
    public void injectBeans() {
        // visit @Autowired fields
        annotationManager.visitFields(SpringAnnotation.AUTOWIRED, this::visitInjectedField);
        annotationManager.visitFields(JavaxAnnotation.RESOURCE, this::visitInjectedField);
        // visit @Bean methods
        annotationManager.visitMethodsInClasses(SpringAnnotation.CONFIGURATION,
            SpringAnnotation.BEAN, (jMethod, ignored) -> this.visitInjectedVarsInMethod(jMethod));
        annotationManager.visitMethodsInClasses(SpringAnnotation.SPRING_BOOT_APPLICATION,
                SpringAnnotation.BEAN, (jMethod, ignored) -> this.visitInjectedVarsInMethod(jMethod));
        annotationManager.visitMethodsInClasses(SpringAnnotation.SPRING_BOOT_CONFIGURATION,
                SpringAnnotation.BEAN, (jMethod, ignored) -> this.visitInjectedVarsInMethod(jMethod));
        // visit @Autowired methods
        annotationManager.visitMethods(SpringAnnotation.AUTOWIRED,
            (jMethod, ignored) -> this.visitInjectedVarsInMethod(jMethod));
        // visit primary constructor of beans
        beanManager.getAllBeans()
                   .stream()
                   .map(Bean::getConstructor)
                   .filter(Objects::nonNull)
                   .forEach(this::visitInjectedVarsInMethod);
    }

    private void visitInjectedField(JField jField, Annotation ignored) {
        // by bean name
        Bean bean = beanManager.getBean(getQualifiedBeanNameOfField(jField));
        if (bean != null) {
            JClass fieldClass = ((ClassType) jField.getType()).getJClass();
            if (!classHierarchy.isSubclass(fieldClass, bean.getJClass())) {
                bean = null;
            }
        }
        // by type
        if (bean == null) {
            bean = beanManager.getBean(((ClassType) jField.getType()).getJClass());
        }
        // TODO: create an UNKNOWN nonCreatedBean
        if (bean != null) {
            bean.addOutEdge(jField);
        }
    }

    /**
     * inject parameter var and this var <br>
     */
    private void visitInjectedVarsInMethod(JMethod jMethod) {
        IR ir = jMethod.getIR();
        // connect PFG to parameter var
        for (int i = 0; i < jMethod.getParamCount(); i++) {
            String beanName = jMethod.getParamAnnotations(i)
                                     .stream()
                                     .filter(anno -> SpringAnnotation.QUALIFIER.name.equals(anno.getType()))
                                     .findFirst()
                                     .map(AnnotationUtils::getStringElement)
                                     .orElse(null);
            beanManager.injectVar(ir.getParam(i), beanName);
        }
        // connect PFG to this var
        Var thisVar = ir.getThis();
        Bean bean = beanManager.getBean(jMethod.getDeclaringClass());
        if (thisVar != null && bean != null) {
            bean.addOutEdge(thisVar);
        }
        // add method to injected methods
        injectedMethods.add(jMethod);
    }

    private String getQualifiedBeanNameOfField(JField jField) {
        String beanName = Optional.ofNullable(jField.getAnnotation(SpringAnnotation.QUALIFIER.getName()))
                                  .map(AnnotationUtils::getStringElement)
                                  .orElse(null);
        if (beanName == null) {
            beanName = Optional.ofNullable(jField.getAnnotation(JavaxAnnotation.RESOURCE.getName()))
                               .map(anno -> AnnotationUtils.getStringElement(anno, "name"))
                               .orElse(null);
        }
        if (beanName == null) {
            beanName = jField.getName();
        }
        return beanName;
    }

    /**
     * if a constructor with @Autowired annotation is found, it is returned <br>
     * else calls {@link BeanConstructorGetter#emptyConstructorFirstGetter}
     */
    public static JMethod autowiredConstructorFirstGetter(JClass beanClass) {
        List<JMethod> autowiredConstructors = beanClass
            .getDeclaredMethods()
            .stream()
            .filter(JMethod::isConstructor)
            .filter(m -> m.getAnnotation(SpringAnnotation.AUTOWIRED.name) != null)
            .toList();
        if (autowiredConstructors.size() == 1) {
            return autowiredConstructors.get(0);
        } else if (autowiredConstructors.size() > 1) {
            throw new AnalysisException("Multiple @Autowired constructors found in: "
                + beanClass.getName());
        }
        return BeanConstructorGetter.emptyConstructorFirstGetter(beanClass);
    }

    public Collection<JMethod> getInjectedMethods() {
        return injectedMethods;
    }
}
