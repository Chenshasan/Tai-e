package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.defuse.DefUse;
import pascal.taie.analysis.defuse.DefUseAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoAPIMisuseAnalysis;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjInformation;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjManager;
import pascal.taie.analysis.pta.plugin.cryptomisuse.IndexUtils;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.CompositeRuleIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.InfluencingFactorIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.InfluencingFactorRule;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class InfluencingFactorRuleJudge implements RuleJudge {

    InfluencingFactorRule influencingFactorRule;

    CryptoObjManager manager;

    ClassHierarchy classHierarchy;

    IntraprocedualDefUse intraprocedualDefUse;

    Logger logger = LogManager.getLogger(InfluencingFactorRuleJudge.class);

    public InfluencingFactorRuleJudge(InfluencingFactorRule influencingFactorRule,
                                      CryptoObjManager manager,
                                      ClassHierarchy classHierarchy) {
        this.influencingFactorRule = influencingFactorRule;
        this.manager = manager;
        this.classHierarchy = classHierarchy;
        this.intraprocedualDefUse = new IntraprocedualDefUse("src/test/resources/pta/cryptomisuse/defuse-config.yml");
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        String index = influencingFactorRule.index();
        String type = influencingFactorRule.type();
        String factor = influencingFactorRule.factor();
        Issue issue = null;
        JMethod container = influencingFactorRule.getMethod();
        Collection<Stmt> stmts = container.getIR().getStmts();
        intraprocedualDefUse.analyze(container.getIR());
        DefUse defUse = container.getIR().getResult(DefUseAnalysis.ID);
        String factorStr = factor.matches("\\d+")
                ? container.getIR().getParam(Integer.parseInt(factor)).toString()
                : factor;
        if (CryptoAPIMisuseAnalysis.getAppClasses().
                contains(influencingFactorRule.method().getDeclaringClass())) {
            if (index.matches("\\d+")) {
                if (type.equals("use")) {
                    Var param = container.getIR().getParam(Integer.parseInt(index));
                    Set<Stmt> useStmts = defUse.getUsesOfParam(param);
                    if (!factorStr.equals("null")) {
                        if (useStmts.stream().noneMatch(stmt -> stmt.toString().contains(factorStr))) {
                            return report(null, null, null);
                        }
                    }
                }
            } else if (index.equals("exception")) {
                AtomicBoolean influence = new AtomicBoolean(false);
                stmts.stream()
                        .filter(stmt -> stmt instanceof Throw)
                        .forEach(stmt -> {
                            Var exceptionVar = ((Throw) stmt).getExceptionRef();
                            defUse.getDefs(stmt, exceptionVar).forEach(defStmt -> {
                                if (defUse.getUses(defStmt).size() > 1) {
                                    influence.set(true);
                                }
                            });
                        });
                influence.set(influence.get() || !container.getExceptions().isEmpty());

                if (!influence.get()) {
                    return report(null, null, null);
                }
            } else {
//            index为stmt
                Set<Stmt> indexes = stmts.stream()
                        .filter(stmt -> stmt.toString().contains(index))
                        .collect(Collectors.toSet());
                Set<Stmt> influenced = indexes.stream()
                        .flatMap(stmt -> type.equals("def")
                                ? intraprocedualDefUse.
                                getInfluencingStmtsBackward(container, stmt).stream()
                                : intraprocedualDefUse.
                                getInfluencingStmtsForward(container, stmt).stream())
                        .collect(Collectors.toSet());
                if (!factorStr.equals("null")) {
                    if (influenced.stream().noneMatch(stmt -> stmt.toString().contains(factorStr))) {
                        return report(null, null, null);
                    }
                }
            }
        }
        return null;
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        String index = influencingFactorRule.index();
        String type = influencingFactorRule.type();
        String factor = influencingFactorRule.factor();
        JMethod method = influencingFactorRule.getMethod();
        String methodStr = method.toString();
        String subsignature = method.getSubsignature().toString();
        String description = "";

        switch (index) {
            case "return":
                description = "Should process the return value of this method instead of returning directly";
                break;
            case "exception":
                description = "Should throw an exception or produce some output based on the exception information in this method";
                break;
            default:
                if (index.matches("\\d+")) {
                    description = "Should " + influencingFactorRule.type();
                }
                break;
        }

        if (!factor.equals("null")) {
            String influencingFactor = factor.matches("\\d+")
                    ? method.getIR().getParam(Integer.parseInt(factor)).toString()
                    : factor;
            description += " with the " + type + " influencing factor: " + influencingFactor;
        }

        InfluencingFactorIssue issue = new InfluencingFactorIssue(
                "Influencing Factor",
                description,
                methodStr,
                subsignature
        );
        return issue;
    }
}
