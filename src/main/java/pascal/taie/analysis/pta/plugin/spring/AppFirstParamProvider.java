/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.spring;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.ParamProvider;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.language.classes.*;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.JClassUtils;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.TwoKeyMultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This {@link ParamProvider} creates parameter objects based on
 * <br/>
 * First Policy: all concrete subtypes of the argument type in the application,
 * including itself if it is concrete.
 * <br/>
 * Fallback Policy: all concrete subtypes of the argument type in the World.
 */
public class AppFirstParamProvider implements ParamProvider {

    private final JMethod method;

    private final ClassHierarchy classHierarchy;

    private final Solver solver;

    public AppFirstParamProvider(JMethod jMethod,
                                 ClassHierarchy classHierarchy,
                                 Solver solver) {
        this.method = jMethod;
        this.classHierarchy = classHierarchy;
        this.solver = solver;
    }

    @Override
    public Set<Obj> getThisObjs() {
        if (method.isStatic() || method.getDeclaringClass().isAbstract()) {
            return Set.of();
        } else {
            return getNonAbstractSubclasses(method.getDeclaringClass())
                    .stream()
                    .map(c -> MockObjUtils.getMockObj(solver, Descriptor.ENTRY_DESC,
                            c.getType(), c.getType(), method))
                    .collect(Collectors.toSet());
        }
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

    @Override
    public TwoKeyMultiMap<Obj, JField, Obj> getFieldObjs() {
        return null;
    }

    @Override
    public MultiMap<Obj, Obj> getArrayObjs() {
        return null;
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
