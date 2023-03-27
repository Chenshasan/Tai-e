package pascal.taie.analysis.pta.plugin.spring;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.plugin.EntryPointHandler;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.spring.mapstruct.MapStructModel;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;

public class SpringAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(SpringAnalysis.class);

    private Solver solver;

    private ClassHierarchy classHierarchy;

    private CSManager csManager;

    private Context emptyContext;

    private MapStructModel mapStructModel;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.classHierarchy = solver.getHierarchy();
        this.csManager = solver.getCSManager();
        this.emptyContext = solver.getContextSelector().getEmptyContext();
        this.mapStructModel = new MapStructModel(solver);
    }

    @Override
    public void onStart() {
        MicroserviceHolder.initialize(solver);

        // add SpringApplication entry points
//        for (MicroserviceHolder ms : MicroserviceHolder.getAllHolders()) {
//            JMethod mainMethod = ms.getMainMethod();
//            if (mainMethod != null) {
//                logger.info("[Spring Analysis] Adding entry point for main method: {}",
//                        mainMethod);
//                solver.addEntryPoint(new EntryPoint(mainMethod,
//                        new EntryPointHandler.MainEntryPointParamProvider(mainMethod, solver)));
//            }
//        }

        // add entry points (the methods in the entry point classes)
        for (JMethod method : MicroserviceHolder.getEntryPoints()) {
            Collection<Obj> thisObjs = MicroserviceHolder.getBeanObjs(method.getDeclaringClass());
            SpecifiedParamProvider.Builder builder = new SpecifiedParamProvider.Builder(method)
                    .setDelegate(new AppFirstParamProvider(method, classHierarchy, solver));
            if (!thisObjs.isEmpty()) {
                builder.addThisObj(thisObjs);
            }
            SpecifiedParamProvider paramProvider = builder.build();
            logger.info("""
                    [Spring Analysis] Adding entry point
                    \tmethod: {}
                    \tparamProvider: {}
                    """, method, paramProvider);
            solver.addEntryPoint(new EntryPoint(method, paramProvider));
        }

        // make bean constructors and methods with @Bean, @Autowired reachable methods
        MicroserviceHolder.getInjectedMethods()
                          .stream()
                          .map(this::getCSMethod)
                          .forEach(solver::addCSMethod);

        // connect PFG for BeanPointer
        MicroserviceHolder.connectPFGEdge(this::connectPFGEdge, this::connectPFGEdge);

        // bean.obj -> bean.pointer
        MicroserviceHolder.propagateBeanObj(this::propagateBeanObj);
    }

    @Override
    public void onFinish() {
        // TODO: get missing injection fields
    }

    @Override
    public void onNewMethod(JMethod method) {
        mapStructModel.handleNewMethod(method);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        mapStructModel.handleNewCSMethod(csMethod);
    }

    @Override
    public void onNewPointsToSet(VirtualPointer virtualPointer, PointsToSet pts) {
        MicroserviceHolder.onNewPointsToSet(virtualPointer, pts, this::injectObjIntoInstanceField);
    }

    /**
     * inject injectedObj to containerObj's jField
     */
    private void injectObjIntoInstanceField(PointsToSet injectedCSObjs,
                                            Obj containerObj,
                                            JField jField) {
        for (CSObj injectedCSObj : injectedCSObjs) {
            logger.info("""
                            [Spring Analysis] Inject obj into field:
                            \tcontainer: {}
                            \tfield: {}
                            \tinjectedObj: {}""",
                    containerObj, jField, injectedCSObj);
            CSManager csManager = solver.getCSManager();
            Context emptyContext = solver.getContextSelector().getEmptyContext();
            CSObj containerCSObj = csManager.getCSObj(emptyContext, containerObj);
            InstanceField instanceField = csManager.getInstanceField(containerCSObj, jField);
            solver.addPointsTo(instanceField, injectedCSObj);
        }
    }

    private void connectPFGEdge(Pointer fromPointer, Var to) {
        CSVar toPointer = csManager.getCSVar(emptyContext, to);
        solver.addPFGEdge(fromPointer, toPointer, FlowKind.OTHER);
    }

    private void connectPFGEdge(Var from, Pointer toPointer) {
        CSVar fromPointer = csManager.getCSVar(emptyContext, from);
        solver.addPFGEdge(fromPointer, toPointer, FlowKind.OTHER);
    }

    private CSMethod getCSMethod(JMethod jMethod) {
        return csManager.getCSMethod(emptyContext, jMethod);
    }

    private void propagateBeanObj(Obj beanObj, Pointer pointer) {
        CSObj csObj = csManager.getCSObj(emptyContext, beanObj);
        solver.addPointsTo(pointer, csObj);
    }
}
