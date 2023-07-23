package pascal.taie.analysis.pta.plugin.owasp;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.ParamProvider;

import java.util.Set;

public class OWASPParamProvider implements ParamProvider {
    @Override
    public Set<Obj> getThisObjs() {
        return Set.of();
    }

    @Override
    public Set<Obj> getParamObjs(int i) {
        return Set.of();
    }
}
