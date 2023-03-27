package pascal.taie.analysis.pta.plugin.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import javax.annotation.Nullable;

/**
 * TODO: better design
 */
public class MockObjUtils {

    private static final Logger logger = LogManager.getLogger(MockObjUtils.class);

    private MockObjUtils() {
    }

    public static Obj getMockObj(Solver solver, Descriptor desc, Object alloc, Type type) {
        return getMockObj(solver, desc, alloc, type, null);
    }

    public static Obj getMockObj(Solver solver, Descriptor desc, Object alloc, Type type,
                                 @Nullable JMethod container) {
        HeapModel heapModel = solver.getHeapModel();
        Obj mockObj = heapModel.getMockObj(desc, alloc, type, container);
        // make mockObject's constructors entrypoint
//        if (type instanceof ClassType classType && !classType.getJClass().isAbstract()) {
//            for (JMethod m : classType.getJClass().getDeclaredMethods()) {
//                if (m.isConstructor()) {
//                    // ad-hoc for JDK class:
//                    // only add entrypoint for constructors with no parameters
//                    if (classType.getJClass().getName().startsWith("java.")
//                            && m.getParamCount() != 0) {
//                        continue;
//                    }
//
//                    logger.info("[Mocking obj] add entrypoint "
//                            + "for mock object's constructor: {}", m);
//                    SpecifiedParamProvider paramProvider = new SpecifiedParamProvider.Builder(m)
//                            .setDelegate(new DeclaredParamProvider(m, heapModel))
//                            .addThisObj(mockObj)
//                            .build();
//                    solver.addEntryPoint(new EntryPoint(m, paramProvider));
//                }
//            }
//        }
        return mockObj;
    }
}
