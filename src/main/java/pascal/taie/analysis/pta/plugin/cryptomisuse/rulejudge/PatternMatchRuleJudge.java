package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.*;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.IssueList;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.PatternMatchIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PatternMatchRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

public class PatternMatchRuleJudge implements RuleJudge {

    PatternMatchRule patternMatchRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(PatternMatchRuleJudge.class);

    public PatternMatchRuleJudge(PatternMatchRule patternMatchRule, CryptoObjManager manager) {
        this.patternMatchRule = patternMatchRule;
        this.manager = manager;
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, patternMatchRule.index());
        IssueList issueList = new IssueList();
        AtomicReference<Issue> issue = new AtomicReference<>();
        if (CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())) {
            result.getPointsToSet(var).stream().
                    filter(manager::isCryptoObj).
                    forEach(cryptoObj -> {
                        if (cryptoObj.getAllocation() instanceof
                                CryptoObjInformation coi) {
                            if (coi.constantValue() instanceof String value) {
                                logger.debug("coi constant value is " + value);
                                match.set(match.get() && !(Pattern.matches(
                                        patternMatchRule.pattern(), value)));
                                // used to be ture and now it is false
                                if (Pattern.matches(patternMatchRule.pattern(), value)) {
                                    issue.set(report(coi, var, callSite));
                                    issueList.addIssue(report(coi, var, callSite));
                                }
                            }
                        }
                    });
            logger.debug("the result of " + callSite + " is " + match.get());
        }
        if(issueList.getIssues().size()>0){
            return issueList;
        }
        else{
            return null;
        }
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        Stmt stmt = coi.allocation();
        PatternMatchIssue issue = new PatternMatchIssue("Pattern Match",
                "The pattern is not matched for the API",
                coi.allocation().toString(),
                coi.sourceMethod().toString(),
                callSite, var.getName(),
                coi.constantValue().toString(), patternMatchRule.method().toString(),
                callSite.getContainer().getSubsignature().toString());
        return issue;
    }
}
