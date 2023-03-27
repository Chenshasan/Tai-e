package pascal.taie.analysis.pta.plugin.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.VirtualPointer;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.spring.enums.SpringAnnotation;
import pascal.taie.analysis.pta.plugin.spring.enums.SpringConfiguration;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.*;
import pascal.taie.util.*;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class MicroserviceHolder {

    private static final Logger logger = LogManager.getLogger(MicroserviceHolder.class);

    private static final Collection<MicroserviceHolder> allHolders = Sets.newSet();

    /* sub-analysis */

    private final XmlConfiguration xmlConfiguration;

    private final YamlConfiguration yamlConfiguration;

    private final PropertiesConfiguration propertiesConfiguration;

    private final AnnotationManager annotationManager;

    private final SpringEntryPointManager springEntryPointManager;

    private final BeanManager beanManager;

    private final SpringIocAnalysis springIocAnalysis;

    private final SpringWebAnalysis springWebAnalysis;

    private final String archivePath;

    private final String classPath;

    private final List<String> dependencyClassPaths;

    private final Set<String> appClasses;

    private final Set<String> classes = Sets.newSet();

    private String serviceName;

    /**
     * trace the rpc callsites
     */
    private final Map<Invoke, String> rpcCallsite2ServiceName = Maps.newMap();

    /**
     * trace the MQ call edge
     */
    private final Set<Edge<CSCallSite, CSMethod>> mqCallEdges = Sets.newSet();

    private Solver solver;

    private ClassHierarchy classHierarchy;

    public MicroserviceHolder(String archivePath,
                              String classPath,
                              List<String> dependencyJarPaths,
                              List<String> appClasses) {
        allHolders.add(this);
        this.archivePath = archivePath;
        this.classPath = classPath;
        this.dependencyClassPaths = dependencyJarPaths;
        this.appClasses = Sets.newSet(appClasses);
        this.initializeClasses();

        logger.info("New MicroserviceHolder ({} classes in total, {} application classes) from {}",
            classes.size(),
            appClasses.size(),
            classPath
        );

        // TODO: need to solve the dependency of *Manager and *Analysis elegantly but not hard processing
        xmlConfiguration = new XmlConfiguration(this.classPath);
        propertiesConfiguration = new PropertiesConfiguration(this.classPath);
        yamlConfiguration = new YamlConfiguration(this.classPath);
        annotationManager = new AnnotationManager(this.appClasses);
        springEntryPointManager = new SpringEntryPointManager();
        beanManager = new BeanManager();
        springIocAnalysis = new SpringIocAnalysis();
        springWebAnalysis = new SpringWebAnalysis();
    }

    public void initializeClasses() {
        classes.addAll(this.appClasses);
        dependencyClassPaths.stream()
                            .map(DirectoryTraverser::listClassesInJar)
                            .forEach(classes::addAll);
    }

    public static void clear() {
        allHolders.clear();
    }

    public static void initialize(Solver solver) {
        // run ordered but microservice-dependent tasks
        allHolders.forEach(holder -> {
            holder.solver = solver;
            holder.classHierarchy = solver.getHierarchy();
            // set holder to sub analysis
            holder.springIocAnalysis.setMicroserviceHolder(holder);
            holder.springWebAnalysis.setMicroserviceHolder(holder);
            holder.springEntryPointManager.setMicroserviceHolder(holder);
            // initialize the configuration
            holder.xmlConfiguration.initialize();
            holder.yamlConfiguration.initialize();
            holder.propertiesConfiguration.initialize();
            // initialize the service name
            holder.initializeServiceName();
            // initialize the annotation manager
            holder.annotationManager.initialize();
            // run the spring web analysis
            holder.springWebAnalysis.initialize();
            holder.springWebAnalysis.logEndpoints();
        });
        // run ordered and microservice-related tasks
        allHolders.forEach(holder -> holder.springIocAnalysis.createBeans());
        allHolders.forEach(holder -> holder.springIocAnalysis.injectBeans());
        allHolders.forEach(holder -> holder.springEntryPointManager.initialize());
    }

    public static Collection<JMethod> getEntryPoints() {
        return allHolders.stream()
                         .map(holder -> holder.springEntryPointManager.getEntrypoint())
                         .flatMap(Collection::stream)
                         .collect(Collectors.toSet());
    }

    public static Collection<JMethod> getInjectedMethods() {
        return allHolders.stream()
                         .map(holder -> holder.springIocAnalysis.getInjectedMethods())
                         .flatMap(Collection::stream)
                         .collect(Collectors.toSet());
    }

    public static Collection<Obj> getBeanObjs(JClass jClass) {
        return allHolders.stream()
                         .map(holder -> holder.beanManager.getBean(jClass))
                         .filter(Objects::nonNull)
                         .map(Bean::getObj)
                         .filter(Objects::nonNull)
                         .collect(Collectors.toSet());
    }

    public static void connectPFGEdge(BiConsumer<Pointer, Var> connectConsumer,
                                      BiConsumer<Var, Pointer> connectConsumer2) {
        allHolders.forEach(holder -> holder.beanManager.connectPFGEdge(connectConsumer, connectConsumer2));
    }

    public static void propagateBeanObj(BiConsumer<Obj, Pointer> propagateConsumer) {
        allHolders.stream()
                  .map(MicroserviceHolder::getBeanManager)
                  .map(BeanManager::getAllBeans)
                  .flatMap(Collection::stream)
                  .filter(bean -> bean.getObj() != null) // TODO: think about it
                  .forEach(bean ->
                      propagateConsumer.accept(bean.getObj(), bean.getPointer())
                  );
    }

    /**
     * inject bean obj into fields
     */
    public static void onNewPointsToSet(VirtualPointer virtualPointer,
                                        PointsToSet pts,
                                        TriConsumer<PointsToSet, Obj, JField> onNewPointsToSetConsumer) {
        for (MicroserviceHolder holder : allHolders) {
            Bean bean = holder.beanManager.getBean(virtualPointer);
            if (bean == null) {
                continue;
            }
            for (JField outField : bean.getOutFields()) {
                Obj containerObj = Optional.ofNullable(holder.beanManager
                                               .getBean(outField.getDeclaringClass()))
                                           .map(Bean::getObj).orElse(null);
                if (containerObj == null) {
                    logger.warn("Container Obj of Field '{}' is null", outField);
                } else {
                    onNewPointsToSetConsumer.accept(pts, containerObj, outField);
                }
            }
        }
    }

    public void initializeServiceName() {
        this.serviceName = propertiesConfiguration.getProperty(SpringConfiguration.APPLICATION_NAME.key);
        if (this.serviceName == null) {
            this.serviceName = yamlConfiguration.getString(SpringConfiguration.APPLICATION_NAME.key);
        }
        if (this.serviceName == null) {
            String name = new File(this.archivePath).getName();
            for (int i = 0; i < name.length(); i++) {
                if (Character.isDigit(name.charAt(i))) {
                    this.serviceName = name.substring(0, i - 1);
                    break;
                }
            }
        }
        if (this.serviceName == null) {
            this.serviceName = "null";
        }
    }

    @Nullable
    public static MicroserviceHolder getSelfMicroserviceHolder(JMethod containerMethod) {
        return getSelfMicroserviceHolder(containerMethod.getDeclaringClass());
    }

    /**
     * TODO: to improve the performance
     */
    @Nullable
    public static MicroserviceHolder getSelfMicroserviceHolder(JClass containerClass) {
        for (MicroserviceHolder holder : allHolders) {
            if (holder.appClasses.contains(containerClass.getName())) {
                return holder;
            }
        }
        return null;
    }


    public boolean traceRpc(Invoke callSite, String targetServiceName) {
        return null == rpcCallsite2ServiceName.put(callSite, targetServiceName);
    }

    public static void reportAllStatistics() {
        StringBuilder sb = new StringBuilder("----------------------------------------\n");
        int webEntriesSum = 0;
        int beansSum = 0;
        for (MicroserviceHolder holder : allHolders) {
            String serviceName = holder.getServiceName();
            int webEntries = holder.springWebAnalysis.getRequestMappingEntries().size();
            int beans = holder.beanManager.size();

            webEntriesSum += webEntries;
            beansSum += beans;

            sb.append(String.format("'%s' has following statistics:\n", serviceName));

            if (webEntries > 0) {
                sb.append(String.format("\tWebEntry: %d\n", webEntries));
            }
            if (beans > 0) {
                sb.append(String.format("\tBean: %d\n", beans));
            }

            String rpcDetails = holder.rpcCallsite2ServiceName
                .values()
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
            if (!rpcDetails.isEmpty()) {
                sb.append(String.format("\tRpcEdge: %s\n", rpcDetails));
            }

            String mqDetails = holder.mqCallEdges
                .stream()
                .map(edge -> MicroserviceHolder.getSelfMicroserviceHolder(edge.getCallee()
                                                                              .getMethod()
                                                                              .getDeclaringClass())
                                               .getServiceName())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
            if (!mqDetails.isEmpty()) {
                sb.append(String.format("\tMqCallEdge: %s\n", mqDetails));
            }
        }
        sb.append("All microservices have following statistics:\n");
        sb.append(String.format("\tWebEntry: %d\n", webEntriesSum));
        sb.append(String.format("\tBean: %d\n", beansSum));
        String appPackagePrefix = AppClassInferringUtils.inferAppPackagePrefix(
                MicroserviceHolder.allHolders.iterator().next().appClasses);
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        long appReachableMethod = pta.getCallGraph()
                                     .reachableMethods()
                                     .filter(jMethod -> jMethod.getDeclaringClass().getName().startsWith(appPackagePrefix))
                                     .count();
        int appMethod = World.get().getClassHierarchy().allClasses()
                             .filter(jClass -> jClass.getName().startsWith(appPackagePrefix))
                             .mapToInt(jClass -> jClass.getDeclaredMethods().size())
                             .sum();
        sb.append(String.format("\tAppReachableMethod (insens): %d\n", appReachableMethod));
        sb.append(String.format("\tAppMethod: %d\n", appMethod));
        sb.append("\tAbove metrics: ")
          .append(webEntriesSum).append('\t')
          .append(beansSum).append('\t')
          .append(appReachableMethod).append('\t')
          .append(appMethod);
        logger.info(sb.toString());
    }

    @Nullable
    public JMethod getMainMethod() {
        JMethod mainMethod = null;
        for (String appClass : appClasses) {
            JClass clz = this.classHierarchy.getClass(appClass);
            if (clz != null &&
                    clz.getAnnotation(SpringAnnotation.SPRING_BOOT_APPLICATION.name) != null) {
                JMethod m = clz.getDeclaredMethod(Subsignature.get(
                        "void main(java.lang.String[])"));
                if (m != null && m.isPublic() && m.isStatic()) {
                    if (mainMethod != null) {
                        logger.error("Multiple main methods found: {}, {}",
                                mainMethod, m);
                    }
                    mainMethod = m;
                }
            }
        }
        return mainMethod;
    }

    public static Collection<MicroserviceHolder> getAllHolders() {
        return allHolders;
    }

    public SpringWebAnalysis getSpringWebAnalysis() {
        return springWebAnalysis;
    }

    public SpringIocAnalysis getSpringIocAnalysis() {
        return springIocAnalysis;
    }

    public Solver getSolver() {
        return solver;
    }

    public XmlConfiguration getXmlConfiguration() {
        return xmlConfiguration;
    }

    public YamlConfiguration getYamlConfiguration() {
        return yamlConfiguration;
    }

    public PropertiesConfiguration getPropertiesConfiguration() {
        return propertiesConfiguration;
    }

    public AnnotationManager getAnnotationManager() {
        return annotationManager;
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Set<String> getAppClasses() {
        return appClasses;
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }
}
