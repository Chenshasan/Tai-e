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

package pascal.taie.util;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Streams;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Some convenient utility methods when debugging. <br>
 * Write them on demand.
 */
public final class DebugUtils {

    private DebugUtils() {
    }

    private static final
    Map<Class<? extends Pointer>, Function<? extends Pointer, String>> pointerClz2StringifyFunc = Map.of(
            CSVar.class, (CSVar o) -> o.toString() + " (" + o.getVar().getType() + ")",
            InstanceField.class, (InstanceField o) -> o.getBase() + "." + o.getField(),
            StaticField.class, (StaticField o) -> o.toString(),
            ArrayIndex.class, (ArrayIndex o) -> o.toString()
    );

    @SuppressWarnings("unchecked")
    private static <T extends Pointer> Function<T, String> getStringify(T pointer) {
        return (Function<T, String>) pointerClz2StringifyFunc.get(pointer.getClass());
    }

    public static void dumpIR(String className) {
        new IRDumper(AnalysisConfig.of(IRDumper.ID))
                .analyze(getJClass(className));
    }

    /**
     * @param className fully-qualified class name
     */
    @Nullable
    public static JClass getJClass(String className) {
        return World.get()
                .getClassHierarchy()
                .getClass(className);
    }

    public static PointerAnalysisResult getPtaResult() {
        return World.get().getResult(PointerAnalysis.ID);
    }

    public static List<CSVar> getCSVars(String varSig) {
        PointerAnalysisResult pta = getPtaResult();
        Predicate<CSVar> predicate;
        int i = varSig.lastIndexOf('/');
        if (i != -1) {
            String methodSig = varSig.substring(0, i);
            String varName = varSig.substring(i + 1);
            predicate = o -> varName.equals(o.getVar().getName())
                    && methodSig.equals(o.getVar().getMethod().getSignature());
        } else {
            predicate = o -> varSig.equals(o.getVar().getMethod().getSignature());
        }
        return pta.getCSVars()
                .stream()
                .filter(predicate)
                .toList();
    }

    public static List<InstanceField> getInstanceFields(String obj) {
        PointerAnalysisResult pta = getPtaResult();
        return pta.getInstanceFields()
                .stream()
                .filter(o -> obj.equals(o.getBase().getObject().toString()))
                .toList();
    }

    public static List<StaticField> getStaticFields(String fieldSig) {
        PointerAnalysisResult pta = getPtaResult();
        return pta.getStaticFields()
                .stream()
                .filter(o -> fieldSig.equals(o.getField().getSignature()))
                .toList();
    }

    public static List<ArrayIndex> getArrayIndexes(String obj) {
        PointerAnalysisResult pta = getPtaResult();
        return pta.getArrayIndexes()
                .stream()
                .filter(o -> obj.equals(o.getArray().getObject().toString()))
                .toList();
    }

    /**
     * TODO
     * @param pointer
     * @return
     */
    public static List<Pointer> getPFGEdges(Pointer pointer) {
        PointerAnalysisResult pta = getPtaResult();
        Set<PointerFlowEdge> outEdges = pointer.getOutEdges();
        return null;
    }

    public static void interact() {
        boolean goon = true;
        while (goon) {
            try {
                switch (inputln("Enter '1' to query var,"
                        + " '2' to query instance field,"
                        + " '3' to query static field,"
                        + " '4' to query array index,"
                        + " 'q' to exit.")) {
                    case "1" -> interactQueryingVar();
                    case "2" -> interactQueryingInstanceField();
                    case "3" -> interactQueryingStaticField();
                    case "4" -> interactQueryingArrayIndex();
                    case "q" -> goon = false;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void interactQueryingVar() {
        while (true) {
            String methodOrVar = inputln("Enter method/var or 'q' to return back:");
            if ("q".equals(methodOrVar)) {
                return;
            }
            interactPointerOp(getCSVars(methodOrVar));
        }
    }

    private static void interactQueryingInstanceField() {
        while (true) {
            String obj = inputln("Enter object or 'q' to return back:");
            if ("q".equals(obj)) {
                return;
            }
            List<InstanceField> instanceFields = getInstanceFields(obj);
            interactPointerOp(instanceFields);
        }
    }

    private static void interactQueryingStaticField() {
        while (true) {
            String fieldSig = inputln("Enter field signature or 'q' to return back:");
            if ("q".equals(fieldSig)) {
                return;
            }
            List<StaticField> staticFields = getStaticFields(fieldSig);
            interactPointerOp(staticFields);
        }
    }

    private static void interactQueryingArrayIndex() {
        while (true) {
            String obj = inputln("Enter object or 'q' to return back:");
            if ("q".equals(obj)) {
                return;
            }
            List<ArrayIndex> arrayIndexes = getArrayIndexes(obj);
            interactPointerOp(arrayIndexes);
        }
    }

    private static <T extends Pointer> void interactPointerOp(List<T> pointers) {
        if (pointers.isEmpty()) {
            println("No pointer.");
            return;
        }
        for (int i = 0; i < pointers.size(); i++) {
            T pointer = pointers.get(i);
            var stringify = getStringify(pointer);
            println(i + ": " + stringify.apply(pointer));
        }
        while (true) {
            String indexStr = inputln("Enter index or 'q' to break:");
            if ("q".equals(indexStr)) {
                return;
            }
            T pointer = pointers.get(Integer.parseInt(indexStr));
            switch (inputln("Enter '1' to query points-to set,"
                    + " 'q' to exit.")) {
                case "1" -> interactOutputPointsToSet(pointer);
                case "q" -> {
                    return;
                }
            }
        }
    }

    private static void interactOutputPointsToSet(Pointer pointer) {
        if (pointer != null
                && pointer.getPointsToSet() != null
                && !pointer.getPointsToSet().isEmpty()) {
            PointsToSet pointsToSet = pointer.getPointsToSet();
            println("The size of points-to set is " + pointsToSet.size() + ".");
            if ("1".equals(inputln("Enter '1' to print points-to set, 'q' to break:"))) {
                for (CSObj csObj : pointsToSet) {
                    println("\t" + csObj);
                }
            }
        }
    }

    /**
     * read a line from the stdin.
     */
    private static String inputln(String prompt) {
        println(prompt);
        Scanner scan = new Scanner(System.in);
        return scan.nextLine().strip();
    }

    /**
     * output and flush the stdout for more timely interactions.
     */
    private static void println(String out) {
        System.out.println(out);
        System.out.flush();
    }

    public static List<Pointer> getSourcesOfPointer(Pointer pointer) {
        PointerAnalysisResult pta = getPtaResult();
        List<Pointer> sources = Streams.<Pointer>concat(
                        pta.getCSVars().stream(),
                        pta.getInstanceFields().stream(),
                        pta.getStaticFields().stream(),
                        pta.getArrayIndexes().stream()
                ).filter(p -> p.getOutEdges().stream().anyMatch(e -> e.target() == pointer))
                .toList();
        return sources;
    }
}
