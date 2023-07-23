package pascal.taie.analysis.pta.plugin.cryptomisuse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.ForbiddenMethodIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.ForbiddenMethodRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.util.concurrent.atomic.AtomicReference;

public class ForbiddenMethodRuleJudge implements RuleJudge {

    ForbiddenMethodRule forbiddenMethodRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(NumberSizeRuleJudge.class);

    ForbiddenMethodRuleJudge(ForbiddenMethodRule forbiddenMethodRule, CryptoObjManager manager) {
        this.forbiddenMethodRule = forbiddenMethodRule;
        this.manager = manager;
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicReference<Issue> issue = new AtomicReference<>();
        if (CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())) {
            issue.set(report(null, null, callSite));
        }
        return issue.get();
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        ForbiddenMethodIssue issue = new ForbiddenMethodIssue("Forbidden Method",
                "The method is forbidden from being used",
                callSite.toString(),
                forbiddenMethodRule.method().toString(),
                callSite.getContainer().getSubsignature().toString());
        return issue;
    }
}
