package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import java.util.Set;

public class CryptoAPIMisuseAnalysis implements Plugin {

    private final MultiMap<JMethod, CryptoObjPropagate> propagates = Maps.newMultiMap();
    private final MultiMap<JMethod, Type> sources = Maps.newMultiMap();
    private final MultiMap<Var, Pair<Var, Type>> cryptoPropagates = Maps.newMultiMap();

    private CryptoObjManager manager;

    private TypeSystem typeSystem;

    private Solver solver;

    private CSManager csManager;

    private Context emptyContext;

    private CryptoRuleJudge cryptoRuleJudge;

    private CryptoAPIMisuseConfig config;


    @Override
    public void setSolver(Solver solver) {
        manager = new CryptoObjManager(solver.getHeapModel());
        config = CryptoAPIMisuseConfig.readConfig(
                solver.getOptions().getString("crypto-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        typeSystem = solver.getTypeSystem();
        csManager = solver.getCSManager();
        emptyContext = solver.getContextSelector().getEmptyContext();
        cryptoRuleJudge = new CryptoRuleJudge();
        this.solver = solver;
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod jMethod = csMethod.getMethod();
        jMethod.getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof AssignStmt<?, ?>) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                if (assignStmt.getDef().isPresent()
                        && assignStmt.getDef().get() instanceof Var
                        && ((Var)assignStmt.getDef().get()).
                        getType().getName().equals("java.lang.String")) {
                    Obj cryptoObj =
                            manager.makeCryptoObj(assignStmt,
                                    typeSystem.getType("java.lang.String"));
                    Var lhs = (Var) assignStmt.getLValue();
                    solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext, cryptoObj);
                }
            }
        });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        cryptoPropagates.get(csVar.getVar()).forEach(p -> {
            Var to = p.first();
            Type type = p.second();
            propagateCryptoObj(pts, csVar.getContext(), to, type);
        });
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod callee = edge.getCallee().getMethod();

        propagates.get(callee).forEach(transfer -> {
            Var from = getVar(callSite, transfer.from());
            Var to = getVar(callSite, transfer.to());
            // when transfer to result variable, and the call site
            // does not have result variable, then "to" is null.
            if (to != null) {
                Type type = transfer.type();
                cryptoPropagates.put(from, new Pair<>(to, type));
                Context ctx = edge.getCallSite().getContext();
                CSVar csFrom = csManager.getCSVar(ctx, from);
                propagateCryptoObj(solver.getPointsToSetOf(csFrom), ctx, to, type);
            }
        });
    }

    private static Var getVar(Invoke callSite, int index) {
        InvokeExp invokeExp = callSite.getInvokeExp();
        return switch (index) {
            case CryptoObjPropagate.BASE -> ((InvokeInstanceExp) invokeExp).getBase();
            case CryptoObjPropagate.RESULT -> callSite.getResult();
            default -> invokeExp.getArg(index);
        };
    }

    private void propagateCryptoObj(PointsToSet pts, Context ctx, Var to, Type type) {
        PointsToSet newCryptoObjs = solver.makePointsToSet();
        pts.objects()
                .map(CSObj::getObject)
                .filter(manager::isCryptoObj)
                .map(manager::getAllocation)
                .map(source -> manager.makeCryptoObj(source, type))
                .map(cryptoObj -> csManager.getCSObj(emptyContext, cryptoObj))
                .forEach(newCryptoObjs::addObject);
        if (!newCryptoObjs.isEmpty()) {
            solver.addVarPointsTo(ctx, to, newCryptoObjs);
        }
    }

    @Override
    public void onFinish() {
        Set<CryptoReport> cryptoReports = cryptoRuleJudge.judgeRules();
        solver.getResult().storeResult(getClass().getName(), cryptoReports);
    }
}
