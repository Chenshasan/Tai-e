package pascal.taie.analysis.pta.plugin.owasp;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.ParamProvider;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.spring.MockObjUtils;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class OWASPParamProvider implements ParamProvider {

    private final JMethod method;

    private final ClassHierarchy classHierarchy;

    private final Solver solver;

    public OWASPParamProvider(JMethod jMethod,
                              ClassHierarchy classHierarchy,
                              Solver solver) {
        this.method = jMethod;
        this.classHierarchy = classHierarchy;
        this.solver = solver;
    }

    @Override
    public Set<Obj> getThisObjs() {
        JClass jClass = method.getDeclaringClass();
        Set<Obj> thisObjs = Sets.newSet();
        thisObjs.add(MockObjUtils.getMockObj(solver, Descriptor.ENTRY_DESC,
                jClass.getType(), jClass.getType(), method));
        return thisObjs;
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        return Set.of();
    }
}
