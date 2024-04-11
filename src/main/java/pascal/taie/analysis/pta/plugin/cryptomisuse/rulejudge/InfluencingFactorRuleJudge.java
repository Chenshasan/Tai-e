package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoAPIMisuseAnalysis;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjInformation;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjManager;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.InfluencingFactorIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.InfluencingFactorRule;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class InfluencingFactorRuleJudge implements RuleJudge {

    InfluencingFactorRule influencingFactorRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(NumberSizeRuleJudge.class);

    public InfluencingFactorRuleJudge(InfluencingFactorRule influencingFactorRule, CryptoObjManager manager) {
        this.influencingFactorRule = influencingFactorRule;
        this.manager = manager;
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        int index = influencingFactorRule.index();
        Issue issue = null;
        Var var;
        if (CryptoAPIMisuseAnalysis.getAppClasses().
                contains(influencingFactorRule.method().getDeclaringClass())) {
            if (influencingFactorRule.index() == -2) {
                Collection<Var> returnVars = Sets.newSet();
                Collection<Stmt> stmts = influencingFactorRule.getMethod().getIR().getStmts();
                for (Stmt stmt : stmts) {
                    if (stmt instanceof Return returnStmt) {
                        returnVars.add(returnStmt.getValue());
                    }
                }

                if(returnVars.size()>1){
                    return null;
                }
                else{
                    boolean influence = false;
                    for (Var returnVar : returnVars) {
                        if (getInfluencesStmtsNum(returnVar) > 1) {
                            influence = true;
                        }
                    }

                    if (!influence) {
                        issue = report(null, null, null);
                    }
                }
            } else {
                var = influencingFactorRule.getMethod().getIR().getParam(index);
                if (getInfluencesStmtsNum(var) < 1) {
                    issue = report(null, null, null);
                }
            }
        }
        return issue;
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        InfluencingFactorIssue issue = new InfluencingFactorIssue("Influencing Factory",
                "Concerned var in this method has no influencing stmts",
                influencingFactorRule.method().toString(),
                influencingFactorRule.method().getSubsignature().toString());
        return issue;
    }

    private int getInfluencesStmtsNum(Var var) {
        IR ir = influencingFactorRule.getMethod().getIR();
        AtomicInteger stmtsNum = new AtomicInteger();
        ir.getStmts().forEach(stmt -> {
            if (stmt.getDef().isPresent() && stmt.getDef().get() instanceof Var defVar) {
                if (defVar.getName().equals(var.getName())) {
                    stmtsNum.getAndIncrement();
                }
            }
        });
        stmtsNum.addAndGet(var.getStoreArrays().size());
        return stmtsNum.get();
    }

}
