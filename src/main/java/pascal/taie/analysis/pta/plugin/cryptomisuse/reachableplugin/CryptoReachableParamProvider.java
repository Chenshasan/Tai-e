package pascal.taie.analysis.pta.plugin.cryptomisuse.reachableplugin;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.ParamProvider;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.spring.MockObjUtils;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.JClassUtils;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CryptoReachableParamProvider implements ParamProvider {

    private final JMethod method;

    private final ClassHierarchy classHierarchy;

    private final Solver solver;

    public CryptoReachableParamProvider(JMethod jMethod,
                                        ClassHierarchy classHierarchy,
                                        Solver solver) {
        this.method = jMethod;
        this.classHierarchy = classHierarchy;
        this.solver = solver;
    }

    @Override
    public Set<Obj> getThisObjs() {
        return Set.of();
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        if (method.getParamType(i) instanceof ReferenceType refType) {
            if (refType instanceof ClassType cType) {
                return getNonAbstractSubclasses(cType.getJClass())
                        .stream()
                        .map(c -> MockObjUtils.getMockObj(solver, Descriptor.ENTRY_DESC,
                                c.getType(), c.getType(), method))
                        .collect(Collectors.toSet());
            }
        }
        return Set.of();
    }

    private Collection<JClass> getNonAbstractSubclasses(JClass jClass) {
        if (ClassNames.OBJECT.equals(jClass.getName())) {
            return List.of(jClass);
        }
        // ad-hoc for JDK interface
        switch (jClass.getName()) {
            // for collection
            case "java.lang.Iterable",
                    "java.util.Collection",
                    "java.util.List" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.ArrayList")));
            }
            case "java.util.Set" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.HashSet")));
            }
            case "java.util.SortedSet",
                    "java.util.NavigableSet" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.TreeSet")));
            }
            case "java.util.SortedMap",
                    "java.util.NavigableMap" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.TreeMap")));
            }
            case "java.util.Map" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.HashMap")));
            }
            case "java.util.Map$Entry" -> {
                return List.of(Objects.requireNonNull(
                                classHierarchy.getJREClass("java.util.AbstractMap$SimpleEntry")),
                        Objects.requireNonNull(
                                classHierarchy.getJREClass("java.util.HashMap$Node")));
            }
            case "java.util.Queue" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.LinkedList")));
            }
            case "java.util.Deque" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.ArrayDeque")));
            }
            // for others
            case "java.util.function.Predicate",
                    "java.util.function.Consumer",
                    "java.util.function.BiFunction",
                    "java.lang.reflect.AnnotatedElement",
                    "java.lang.ClassLoader",
                    "java.util.concurrent.TimeUnit",
                    "java.lang.reflect.Type" -> {
                return List.of();
            }
            case "java.lang.Runnable" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.lang.Thread")));
            }
            case "java.util.concurrent.RejectedExecutionHandler" -> {
                return List.of(Objects.requireNonNull(
                                classHierarchy.getJREClass("java.util.concurrent.ThreadPoolExecutor$AbortPolicy")),
                        Objects.requireNonNull(
                                classHierarchy.getJREClass("java.util.concurrent.ThreadPoolExecutor$CallerRunsPolicy")),
                        Objects.requireNonNull(
                                classHierarchy.getJREClass("java.util.concurrent.ThreadPoolExecutor$DiscardOldestPolicy")),
                        Objects.requireNonNull(
                                classHierarchy.getJREClass("java.util.concurrent.ThreadPoolExecutor$DiscardPolicy")));
            }
            case "java.util.concurrent.Executor",
                    "java.util.concurrent.ExecutorService" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.concurrent.ThreadPoolExecutor")));
            }
            case "java.util.concurrent.ThreadFactory" -> {
                return List.of(Objects.requireNonNull(
                        classHierarchy.getJREClass("java.util.concurrent.Executors$DefaultThreadFactory")));
            }
        }
        // ad-hoc: for special cases
        if (jClass.getName().startsWith("org.springframework.data.redis.core.RedisCallback")) {
            return List.of();
        }

        // First Policy
        Collection<JClass> subclasses = classHierarchy.getAllSubclassesOf(jClass);
        Set<JClass> appSubclasses = subclasses
                .stream()
                .filter(c -> c.isApplication() || c.equals(jClass))
                .filter(Predicate.not(JClass::isAbstract))
                .filter(Predicate.not(JClass::isInterface))
                .filter(c -> JClassUtils.isApplicationInMicroservices(c) || c.equals(jClass))
                .collect(Collectors.toSet());
        if (!appSubclasses.isEmpty()) {
            return appSubclasses;
        }
        // Fallback Policy
        return subclasses.stream()
                .filter(Predicate.not(JClass::isAbstract))
                .filter(Predicate.not(JClass::isInterface))
                .collect(Collectors.toSet());
    }
}
