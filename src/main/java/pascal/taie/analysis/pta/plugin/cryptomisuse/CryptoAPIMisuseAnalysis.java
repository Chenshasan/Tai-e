package pascal.taie.analysis.pta.plugin.cryptomisuse;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.CompositeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.FromSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.ToSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.Rule;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CryptoAPIMisuseAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(CryptoAPIMisuseAnalysis.class);

    private static Set<String> appClassesInString = Sets.newSet();

    public static File outputFile() {
        return outputFile;
    }

    private static File outputFile = new File("CryptoAnalysis.json");


    private static Set<JClass> appClasses = Sets.newSet();
    private final MultiMap<JMethod, CryptoObjPropagate> propagates = Maps.newMultiMap();
    private final MultiMap<JMethod, CryptoSource> sources = Maps.newMultiMap();
    private final MultiMap<Var, Pair<Var, Type>> cryptoVarPropagates = Maps.newMultiMap();

    private final MultiMap<Var, Pair<Var, Type>> compositeVarPropagates = Maps.newMultiMap();
    private final Map<Rule, RuleJudge> ruleToJudge = Maps.newMap();

    private final MultiMap<FromSource, CompositeRule> fromSourceToRule = Maps.newMultiMap();

    private final MultiMap<Var, CompositeRule> fromVarToRule = Maps.newMultiMap();
    private final MultiMap<JMethod, FromSource> compositeFromSources = Maps.newMultiMap();

    private final Map<ToSource, CompositeRule> toSourceToRule = Maps.newMap();

    private final MultiMap<JMethod, ToSource> compositeToSources = Maps.newMultiMap();

    private final MultiMap<JMethod, CryptoObjPropagate> compositePropagates = Maps.newMultiMap();

    private final MultiMap<Var, Var> elementToBase = Maps.newMultiMap();

    private final Set<CompositeRule> compositeRules = Sets.newSet();

    private final String PREDICTABLE_DESC = "PredictableSourceObj";

    private CryptoObjManager manager;

    private TypeSystem typeSystem;

    private Solver solver;

    private CSManager csManager;

    private Context emptyContext;

    private CryptoRuleJudge cryptoRuleJudge;

    private CryptoAPIMisuseConfig config;


    public static void addAppClass(Set<String> appClass) {
        appClassesInString = appClass;
    }

    public static Set<JClass> getAppClasses() {
        return appClasses;
    }

    @Override
    public void setSolver(Solver solver) {
        manager = new CryptoObjManager(solver.getHeapModel());
        config = CryptoAPIMisuseConfig.readConfig(
                solver.getOptions().getString("crypto-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        if (solver.getOptions().getString("crypto-output") != null) {
            outputFile = new File(solver.getOptions().getString("crypto-output"));
        }
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
        config.forbiddenMethodRules().forEach(f ->
                ruleToJudge.put(f, new ForbiddenMethodRuleJudge(f, manager)));
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
    public void onStart() {
        ClassHierarchy hierarchy = solver.getHierarchy();
        appClasses.addAll(appClassesInString.stream().
                map(hierarchy::getClass).collect(Collectors.toSet()));
        JClass objectClass = hierarchy.getClass("java.lang.Object");
        Collection<JClass> concernedClass = Sets.newSet();
        concernedClass.addAll(sources.keySet().stream().
                map(ClassMember::getDeclaringClass).collect(Collectors.toSet()));
        concernedClass.addAll(propagates.keySet().stream().
                map(ClassMember::getDeclaringClass).collect(Collectors.toSet()));
        concernedClass.addAll(ruleToJudge.keySet().stream().
                map(rule -> rule.getMethod().getDeclaringClass()).collect(Collectors.toSet()));
        concernedClass.addAll(appClasses);
        //concernedClass.forEach(jClass -> System.out.println("-------------" + jClass));
        hierarchy.getAllSubclassesOf(objectClass).forEach(
                jClass -> {
                    if (!((jClass.getName().contains("java.util"))
                            || concernedClass.contains(jClass))) {
                        jClass.getDeclaredMethods().forEach(jMethod -> {
                            solver.addIgnoredMethod(jMethod);
                        });
                    } else {
                        logger.info(jClass);
                    }
                }
        );
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod jMethod = csMethod.getMethod();
        //logger.info(jMethod.getDeclaringClass().getName());
        Context ctx = csMethod.getContext();
        if(jMethod.getSignature().equals("<com.bwssystems.HABridge.BridgeSecurity: java.lang.String encrypt(java.lang.String)>")){
            logger.info("run to this");
        }
        if (jMethod.getDeclaringClass().isApplication()) {
            jMethod.getIR().getStmts().forEach(stmt -> {
                if (stmt instanceof AssignLiteral assignStmt) {
                    Var lhs = assignStmt.getLValue();
                    if (assignStmt.getRValue() instanceof StringLiteral stringLiteral) {
                        CryptoObjInformation coi =
                                new CryptoObjInformation(stmt, jMethod, stringLiteral.getString());
                        Obj cryptoObj = manager.makeCryptoObj(coi, stringLiteral.getType());
                        logger.debug("Create String Object in Method" + jMethod);
                        solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext,
                                cryptoObj);
                    }

                    if (assignStmt.getRValue() instanceof IntLiteral intLiteral) {
                        CryptoObjInformation coi =
                                new CryptoObjInformation(stmt, jMethod, intLiteral.getValue());
                        Obj cryptoObj = manager.makeCryptoObj(coi, PrimitiveType.INT);
                        logger.debug("Create Integer Object in Method" + jMethod);
                        solver.addVarPointsTo(csMethod.getContext(), lhs, emptyContext,
                                cryptoObj);
                    }
                }

                if (stmt instanceof StoreArray storeArray) {
                    Var base = storeArray.getLValue().getBase();
                    Var rhs = storeArray.getRValue();
                    elementToBase.put(rhs, base);
                    if (rhs.isConst()) {
                        CryptoObjInformation coi =
                                new CryptoObjInformation(stmt, jMethod, PREDICTABLE_DESC);
                        Obj cryptoObj = manager.makeCryptoObj(coi, base.getType());
                        solver.addVarPointsTo(ctx, base, emptyContext, cryptoObj);
                        logger.debug("the store array stmt " + storeArray + "is unsafe with type " + base.getType());
                    }
                }
            });
        }
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

        elementToBase.get(var).forEach(base -> {
            pts.objects()
                    .map(CSObj::getObject)
                    .filter(manager::isCryptoObj)
                    .map(manager::getAllocationOfCOI)
                    .map(source -> manager.makeCryptoObj(source, base.getType()))
                    .map(cryptoObj -> csManager.getCSObj(emptyContext, cryptoObj))
                    .forEach(csObj -> solver.addVarPointsTo(csVar.getContext(), base, csObj));
        });
        addJudgeStmtFromPts(var, pts);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod callee = edge.getCallee().getMethod();

        if (sources.containsKey(callee)) {
            sources.get(callee).forEach(source -> {
                Var var = IndexUtils.getVar(callSite, source.index());
                CryptoObjInformation coi =
                        new CryptoObjInformation(callSite, callSite.getContainer(), PREDICTABLE_DESC);
                Type type = source.type();
                Obj cryptoObj = manager.makeCryptoObj(coi, type);
                solver.addVarPointsTo(edge.getCallSite().getContext(), var,
                        emptyContext, cryptoObj);
            });
        }

        if (compositeFromSources.containsKey(callee)) {
            compositeFromSources.get(callee).forEach(compositeSource -> {
                Var var = IndexUtils.getVar(callSite, compositeSource.index());
                fromSourceToRule.get(compositeSource).forEach(compositeRule -> {
                    CompositeRule cloneCompositeRule = compositeRule.clone();
                    cloneCompositeRule.setFromVar(var);
                    fromVarToRule.put(var, cloneCompositeRule);
                    Type type = compositeSource.type();
                    Obj compositeObj = manager.makeCompositeCryptoObj(cloneCompositeRule, type);
                    solver.addVarPointsTo(edge.getCallSite().getContext(), var,
                            emptyContext, compositeObj);
                });
                logger.debug("generate from var when call method: " + callee + " on stmt: " + callSite);
            });
        }

        if (compositeToSources.containsKey(callee)) {
            compositeToSources.get(callee).forEach(toSource -> {
                Var var = IndexUtils.getVar(callSite, toSource.index());
                CompositeRule compositeRule = toSourceToRule.get(toSource);
                compositeRule.getToVarToStmt().put(var, callSite);
                compositeRule.getToSourceToToVar().put(var, toSource);
                compositeRule.getJudgeStmts().put(callSite, toSource);
                logger.debug("generate to var when call method: " + callee + " on stmt: " + callSite);
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
                        logger.debug("add judge stmt: " + ToVarToStmt.get(var) + "of to var: " + var);
                    }
                });
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
        List<Issue> issueList = new ArrayList<>();
        fromVarToRule.forEach((var, compositeRule) -> {
            CompositeRuleJudge judge = new CompositeRuleJudge(compositeRule, manager);
            compositeRule.getToVarToStmt().forEach((toVar, stmt) -> {
                issueList.add(judge.judge(result, (Invoke) stmt));
            });
        });

        ruleToJudge.forEach((rule, ruleJudge) -> {
            result.getCallGraph()
                    .getCallersOf(rule.getMethod())
                    .forEach(callSite -> {
                        Issue issue = ruleJudge.judge(result, callSite);
                        if (issue != null) {
                            issueList.add(issue);
                        }
                    });
        });

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().
                    writeValue(CryptoAPIMisuseAnalysis.outputFile(), issueList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Set<CryptoReport> cryptoReports = cryptoRuleJudge.judgeRules();
        solver.getResult().storeResult(getClass().getName(), cryptoReports);
    }
}
