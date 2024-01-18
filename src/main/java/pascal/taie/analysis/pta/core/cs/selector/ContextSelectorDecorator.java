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

package pascal.taie.analysis.pta.core.cs.selector;


import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * for modeling the RabbitMQ Binding
 */
public class ContextSelectorDecorator implements ContextSelector {

    private static final Collection<String> CONCERNED_CLASS_PREFIXES = List.of(
            "java.util.HashMap"
    );

    private static final int limit = 4;

    private static final int hlimit = limit - 1;

    private final AbstractContextSelector<Object> selector;

    private final Map<JClass, Boolean> cache = Maps.newMap();

    private boolean isConcerned(JClass jClass) {
        if (jClass == null) {
            return false;
        }
        return cache.computeIfAbsent(jClass, c -> {
            String className = c.getName();
            for (String packageName : CONCERNED_CLASS_PREFIXES) {
                if (className.startsWith(packageName)) {
                    return true;
                }
            }
            return false;
        });
    }

    private boolean isConcerned(CSMethod csMethod) {
        return csMethod != null && isConcerned(csMethod.getMethod().getDeclaringClass());
    }

    @SuppressWarnings("unchecked")
    public ContextSelectorDecorator(ContextSelector selector) {
        // FIXME: the down cast
        this.selector = (AbstractContextSelector<Object>) selector;
    }

    @Override
    public Context getEmptyContext() {
        return selector.getEmptyContext();
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        if (obj instanceof NewObj) {
            return selectNewObjContext(method, (NewObj) obj);
        } else {
            return getEmptyContext();
        }
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        if (isConcerned(callSite.getContainer())
                || isConcerned(callee.getDeclaringClass())) {
            // use call-site sensitive selector for static calls
            return selector.factory.append(callSite.getContext(), callSite.getCallSite(), limit);
        }
        return selector.selectContext(callSite, callee);
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        if (isConcerned(callSite.getContainer())
                || isConcerned(callee.getDeclaringClass())
                || (recv.getObject().getType() instanceof JClass jClass && isConcerned(jClass))) {
            // use object-site sensitive selector for instance calls
            return selector.factory.append(recv.getContext(), recv.getObject(), limit);
        }
        return selector.selectContext(callSite, recv, callee);
    }

    protected Context selectNewObjContext(CSMethod method, NewObj obj) {
        if (isConcerned(method)
                || (obj.getType() instanceof JClass jClass && isConcerned(jClass))) {
            // use object-site sensitive heap
            return selector.factory.makeLastK(method.getContext(), hlimit);
        }
        return selector.selectHeapContext(method, obj);
    }
}
