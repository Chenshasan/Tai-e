package pascal.taie.analysis.pta.plugin.cryptomisuse.reachableplugin;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.ParamProvider;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.spring.MockObjUtils;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.collection.Sets;

import java.util.Set;
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
                Obj obj = MockObjUtils.getMockObj(
                        solver, Descriptor.ENTRY_DESC, cType, cType, method);
                Set<Obj> result = Sets.newSet();
                result.add(obj);
                return result;
            }
        }
        return Set.of();
    }
}
