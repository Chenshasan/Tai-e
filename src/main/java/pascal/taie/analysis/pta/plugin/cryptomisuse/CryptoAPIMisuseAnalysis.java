package pascal.taie.analysis.pta.plugin.cryptomisuse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PatternMatchRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.Rule;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;

public class CryptoAPIMisuseAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(CryptoAPIMisuseAnalysis.class);
    private final MultiMap<JMethod, CryptoObjPropagate> propagates = Maps.newMultiMap();
    private final MultiMap<JMethod, CryptoSource> sources = Maps.newMultiMap();
    private final MultiMap<Var, Pair<Var, Type>> cryptoPropagates = Maps.newMultiMap();
    private final Map<Rule, RuleJudge> ruleToJudge = Maps.newMap();

    private final String PREDICTABLE_DESC = "PredictableSourceObj";

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
        config.sources().forEach(s -> sources.put(s.method(), s));
        config.propagates().forEach(p -> propagates.put(p.method(), p));
        config.patternMatchRules().forEach(pa ->
                ruleToJudge.put(pa, new PatternMatchRuleJudge(pa, manager)));
        config.predictableSourceRules().forEach(pr ->
                ruleToJudge.put(pr, new PredictableSourceRuleJudge(pr, manager)));
        config.numberSizeRules().forEach(n->
                ruleToJudge.put(n, new NumberSizeRuleJudge(n, manager)));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod jMethod = csMethod.getMethod();
        jMethod.getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof AssignLiteral assignStmt) {
                Var lhs = assignStmt.getLValue();
                if (assignStmt.getRValue() instanceof StringLiteral stringLiteral) {
                    CryptoObjInformation coi =
                            new CryptoObjInformation(stmt, stringLiteral.getString());
                    Obj cryptoObj =
                            manager.makeCryptoObj(
                                    coi,
                                    typeSystem.getType("java.lang.String")
                            );
                    // logger.info("new Var: " + lhs.getName() + " in method " + jMethod.getName() + " with String object");
                    solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext,
                            cryptoObj);
                }

                if (assignStmt.getRValue() instanceof IntLiteral intLiteral) {
                    CryptoObjInformation coi =
                            new CryptoObjInformation(stmt, intLiteral.getValue());
                    Obj cryptoObj =
                            manager.makeCryptoObj(
                                    coi,
                                    PrimitiveType.INT
                            );
                    // logger.info("new Var: " + lhs.getName() + " in method " + jMethod.getName() + " with String object");
                    solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext,
                            cryptoObj);
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

        if (sources.containsKey(callee)) {
            sources.get(callee).forEach(source -> {
                Var var = IndexUtils.getVar(callSite, source.index());
                CryptoObjInformation coi =
                        new CryptoObjInformation(callSite, PREDICTABLE_DESC);
                Type type = source.type();
                Obj taint = manager.makeCryptoObj(coi, type);
                solver.addVarPointsTo(edge.getCallSite().getContext(), var,
                        emptyContext, taint);
            });
        }

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
                .map(source -> manager.makeCryptoObj((CryptoObjInformation) source, type))
                .map(cryptoObj -> csManager.getCSObj(emptyContext, cryptoObj))
                .forEach(newCryptoObjs::addObject);
        if (!newCryptoObjs.isEmpty()) {
            solver.addVarPointsTo(ctx, to, newCryptoObjs);
        }
    }

    @Override
    public void onFinish() {
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        PointerAnalysisResult result = solver.getResult();
        ruleToJudge.keySet().forEach(rule -> {
            result.getCallGraph().getCallersOf(rule.getMethod()).
                    forEach(
                            callSite -> ruleToJudge.
                                    get(rule).
                                    judge(result, callSite));
        });
//        config.getCryptoAPIs().forEach(cryptoAPI -> {
//            int i = cryptoAPI.index();
//            result.getCallGraph()
//                    .getCallersOf(cryptoAPI.method())
//                    .forEach(sinkCall -> {
//                        Var arg = sinkCall.getInvokeExp().getArg(i);
//                        result.getPointsToSet(arg)
//                                .stream()
//                                .filter(manager::isCryptoObj)
//                                .forEach(cryptoObj -> {
//                                    System.out.println(arg + "in statement "
//                                            + sinkCall.getInvokeExp()
//                                            + "point to" + cryptoObj);
//                                });
//                    });
//        });
        Set<CryptoReport> cryptoReports = cryptoRuleJudge.judgeRules();
        solver.getResult().storeResult(getClass().getName(), cryptoReports);
    }
}
