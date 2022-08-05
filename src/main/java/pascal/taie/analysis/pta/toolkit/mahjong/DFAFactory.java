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

package pascal.taie.analysis.pta.toolkit.mahjong;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.type.Type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

class DFAFactory {

    private final FieldPointsToGraph fpg;
    protected Set<DFAState> states, visited;
    private Map<Set<Obj>, DFAState> stateMap;

    DFAFactory(FieldPointsToGraph fpg) {
        this.fpg = fpg;
        buildAllDFA();
    }

    DFA getDFA(Obj obj) {
        DFAState q0 = stateMap.get(Set.of(obj));
        return new DFA(q0);
    }

    private void buildAllDFA() {
        stateMap = new HashMap<>();
        states = new HashSet<>();
        visited = new HashSet<>();
        fpg.getObjects().forEach(this::buildDFA);
    }

    /**
     * Perform subset construction algorithm to convert an NFA
     * to a DFA. If a set of NFA states are merged to an existing
     * DFA state, then reused the existing DFA state instead of creating
     * an equivalent new one.
     *
     * @param obj the start state (object) of the DFA
     */
    private void buildDFA(Obj obj) {
        Set<Obj> q0Set = Set.of(obj);
        if (!stateMap.containsKey(q0Set)) {
            NFA nfa = new NFA(obj, fpg);
            DFAState startState = getDFAState(q0Set, nfa);
            Queue<DFAState> worklist = new LinkedList<>();
            states.add(startState);
            worklist.add(startState);
            while (!worklist.isEmpty()) {
                DFAState s = worklist.poll();
                if (!visited.contains(s)) {
                    visited.add(s);
                    Set<Field> fields = fields(nfa, s.getObjects());
                    fields.forEach(f -> {
                        Set<Obj> nextNFAStates = move(nfa, s.getObjects(), f);
                        DFAState nextState = getDFAState(nextNFAStates, nfa);
                        if (!states.contains(nextState)) {
                            states.add(nextState);
                            worklist.add(nextState);
                        }
                        addTransition(s, f, nextState);
                    });
                }
            }
        }
    }

    private DFAState getDFAState(Set<Obj> objs, NFA nfa) {
        if (!stateMap.containsKey(objs)) {
            Set<Type> output = objs.stream()
                    .map(nfa::outputOf)
                    .collect(Collectors.toSet());
            stateMap.put(objs, new DFAState(objs, output));
        }
        return stateMap.get(objs);
    }

    private Set<Obj> move(NFA nfa, Set<Obj> objs, Field f) {
        return objs.stream()
                .map(obj -> nfa.nextStates(obj, f))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<Field> fields(NFA nfa, Set<Obj> objs) {
        return objs.stream()
                .map(nfa::outEdgesOf)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private void addTransition(DFAState s, Field f, DFAState nextState) {
        s.addTransition(f, nextState);
    }
}
