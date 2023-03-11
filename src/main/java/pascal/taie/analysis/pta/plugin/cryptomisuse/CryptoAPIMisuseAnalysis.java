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
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositeRule.CompositeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositeRule.FromSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositeRule.ToSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.NumberSizeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PatternMatchRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.Rule;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.AssignLiteral;
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
    private final MultiMap<Var, Pair<Var, Type>> cryptoVarPropagates = Maps.newMultiMap();

    private final MultiMap<Var, Pair<Var, Type>> compositeVarPropagates = Maps.newMultiMap();
    private final Map<Rule, RuleJudge> ruleToJudge = Maps.newMap();

    private final Map<FromSource, CompositeRule> fromSourceToRule = Maps.newMap();

    private final MultiMap<Var, CompositeRule> fromVarToRule = Maps.newMultiMap();
    private final MultiMap<JMethod, FromSource> compositeFromSources = Maps.newMultiMap();

    private final Map<ToSource, CompositeRule> toSourceToRule = Maps.newMap();

    private final MultiMap<JMethod, ToSource> compositeToSources = Maps.newMultiMap();

    private final MultiMap<JMethod, CryptoObjPropagate> compositePropagates = Maps.newMultiMap();

    private final Set<CompositeRule> compositeRules = Sets.newSet();

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
        config.numberSizeRules().forEach(n ->
                ruleToJudge.put(n, new NumberSizeRuleJudge(n, manager)));
        config.compositeRules().forEach(cr -> {
            compositeRules.add(cr);
            fromSourceToRule.put(cr.getFromSource(), cr);
            compositeFromSources.put(cr.getFromSource().method(), cr.getFromSource());
            cr.getToSources().forEach(toSource -> {
                toSourceToRule.put(toSource, cr);
                compositeToSources.put(toSource.method(), toSource);
            });
            cr.getTransfers().forEach(propagates ->
                    compositePropagates.put(propagates.method(), propagates));
        });
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
                    Obj cryptoObj = manager.makeCryptoObj(coi,
                            typeSystem.getType("java.lang.String"));
                    solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext,
                            cryptoObj);
                }

                if (assignStmt.getRValue() instanceof IntLiteral intLiteral) {
                    CryptoObjInformation coi =
                            new CryptoObjInformation(stmt, intLiteral.getValue());
                    Obj cryptoObj = manager.makeCryptoObj(coi, PrimitiveType.INT);
                    solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext,
                            cryptoObj);
                }
            }
        });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();
        cryptoVarPropagates.get(var).forEach(p -> {
            Var to = p.first();
            Type type = p.second();
            propagateCryptoObj(pts, csVar.getContext(), to, type, false);
        });
        compositeVarPropagates.get(var).forEach(p -> {
            Var to = p.first();
            Type type = p.second();
            propagateCryptoObj(pts, csVar.getContext(), to, type, true);
        });
        addJudgeStmtFromPts(var, pts);
    }

    private void addJudgeStmtFromPts(Var var, PointsToSet pts) {
        pts.objects()
                .map(CSObj::getObject)
                .filter(manager::isCompositeCryptoObj)
                .map(manager::getAllocationOfRule)
                .forEach(compositeRule -> {
                    Map<Var, Stmt> ToVarToStmt = compositeRule.getToVarToStmt();
                    if (ToVarToStmt.containsKey(var)) {
                        ToSource toSource = compositeRule.getToSourceToToVar().get(var);
                        compositeRule.getJudgeStmts().put(ToVarToStmt.get(var), toSource);
                        System.out.println("add judge stmt: " + ToVarToStmt.get(var) + "of to var: " + var);
                    }
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
                Obj cryptoObj = manager.makeCryptoObj(coi, type);
                solver.addVarPointsTo(edge.getCallSite().getContext(), var,
                        emptyContext, cryptoObj);
            });
        }

        if (compositeFromSources.containsKey(callee)) {
            compositeFromSources.get(callee).forEach(compositeSource -> {
                Var var = IndexUtils.getVar(callSite, compositeSource.index());
                CompositeRule compositeRule = fromSourceToRule.get(compositeSource).clone();
                compositeRule.setFromVar(var);
                fromVarToRule.put(var, compositeRule);
                Type type = compositeSource.type();
                Obj compositeObj = manager.makeCompositeCryptoObj(compositeRule, type);
                solver.addVarPointsTo(edge.getCallSite().getContext(), var,
                        emptyContext, compositeObj);
                System.out.println("generate from var when call method: " + callee + " on stmt: " + callSite);
            });
        }

        if (compositeToSources.containsKey(callee)) {
            compositeToSources.get(callee).forEach(toSource -> {
                Var var = IndexUtils.getVar(callSite, toSource.index());
                CompositeRule compositeRule = toSourceToRule.get(toSource);
                compositeRule.getToVarToStmt().put(var, callSite);
                compositeRule.getToSourceToToVar().put(var, toSource);
                compositeRule.getJudgeStmts().put(callSite, toSource);
                System.out.println("generate to var when call method: " + callee + " on stmt: " + callSite);
                Context ctx = edge.getCallSite().getContext();
                CSVar csVar = csManager.getCSVar(ctx, var);
                addJudgeStmtFromPts(var, solver.getPointsToSetOf(csVar));
            });
        }

        propagateOnCallEdge(edge, callSite, callee, propagates,
                cryptoVarPropagates, false);
        propagateOnCallEdge(edge, callSite, callee, compositePropagates,
                compositeVarPropagates, true);
    }

    private void propagateOnCallEdge(Edge<CSCallSite, CSMethod> edge,
                                     Invoke callSite,
                                     JMethod callee,
                                     MultiMap<JMethod, CryptoObjPropagate> cryptoPropagates,
                                     MultiMap<Var, Pair<Var, Type>> cryptoVarPropagates,
                                     boolean isComposite) {
        cryptoPropagates.get(callee).forEach(propagate -> {
            Var from = getVar(callSite, propagate.from());
            Var to = getVar(callSite, propagate.to());
            // when transfer to result variable, and the call site
            // does not have result variable, then "to" is null.
            if (to != null) {
                Type type = propagate.type();
                cryptoVarPropagates.put(from, new Pair<>(to, type));
                Context ctx = edge.getCallSite().getContext();
                CSVar csFrom = csManager.getCSVar(ctx, from);
                propagateCryptoObj(solver.getPointsToSetOf(csFrom), ctx, to, type, isComposite);
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

    private void propagateCryptoObj(PointsToSet pts, Context ctx,
                                    Var to, Type type, boolean isComposite) {
        PointsToSet newCryptoObjs = solver.makePointsToSet();
        if (isComposite) {
            pts.objects()
                    .map(CSObj::getObject)
                    .filter(manager::isCompositeCryptoObj)
                    .map(manager::getAllocationOfRule)
                    .map(source -> manager.makeCompositeCryptoObj(source, type))
                    .map(cryptoObj -> csManager.getCSObj(emptyContext, cryptoObj))
                    .forEach(newCryptoObjs::addObject);
        } else {
            pts.objects()
                    .map(CSObj::getObject)
                    .filter(manager::isCryptoObj)
                    .map(manager::getAllocationOfCOI)
                    .map(source -> manager.makeCryptoObj(source, type))
                    .map(cryptoObj -> csManager.getCSObj(emptyContext, cryptoObj))
                    .forEach(newCryptoObjs::addObject);
        }
        if (!newCryptoObjs.isEmpty()) {
            solver.addVarPointsTo(ctx, to, newCryptoObjs);
        }
    }

    @Override
    public void onFinish() {
        ClassHierarchy classHierarchy = World.get().getClassHierarchy();
        PointerAnalysisResult result = solver.getResult();
        fromVarToRule.forEach((var, compositeRule) -> {
            CompositeRuleJudge judge = new CompositeRuleJudge(compositeRule, manager);
            compositeRule.getToVarToStmt().forEach((toVar, stmt) -> {
                judge.judge(result, (Invoke) stmt);
            });
        });
        ruleToJudge.keySet().forEach(rule -> {
            result.getCallGraph().getCallersOf(rule.getMethod()).
                    forEach(
                            callSite -> ruleToJudge.
                                    get(rule).
                                    judge(result, callSite));
        });

        Set<CryptoReport> cryptoReports = cryptoRuleJudge.judgeRules();
        solver.getResult().storeResult(getClass().getName(), cryptoReports);
    }
}
