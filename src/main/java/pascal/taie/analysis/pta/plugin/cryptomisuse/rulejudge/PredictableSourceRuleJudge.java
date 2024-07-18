package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.*;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.PredictableSourceIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PredictableSourceRuleJudge implements RuleJudge {

    private final String PREDICTABLE_DESC = "PredictableSourceObj";

    PredictableSourceRule predictableSourceRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(PredictableSourceRuleJudge.class);

    public PredictableSourceRuleJudge(PredictableSourceRule predictableSourceRule,
                                      CryptoObjManager manager) {
        this.predictableSourceRule = predictableSourceRule;
        this.manager = manager;
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, predictableSourceRule.index());
        AtomicReference<Issue> issue = new AtomicReference<>();
        if (CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())) {
            result.getPointsToSet(var).
                    stream().
                    filter(manager::isCryptoObj).
                    forEach(cryptoObj -> {
                        if (cryptoObj.getAllocation() instanceof CryptoObjInformation coi) {
                            //String desc = (String) coi.constantValue();
                            if (match.get()) {
                                issue.set(report(coi, var, callSite));
                                logger.debug("the result of " + callSite
                                        + " of var: " + var
                                        + " is false"
                                        + " with crypto obj in"
                                        + ((CryptoObjInformation) cryptoObj.getAllocation()).allocation());
                            }
                            if (coi.constantValue() instanceof String str
                                    && str.equals(PREDICTABLE_DESC)) {
                                issue.set(report(coi, var, callSite));
                            }
                            match.set(false);
                        }
                    });
            if (result.getPointsToSet(var).
                    stream().
                    filter(manager::isPredictableCryptoObj).toList().size() > 0) {
                issue.set(report(null, var, callSite));
            }
        }
        return issue.get();
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        PredictableSourceIssue issue;
        if (coi == null) {
            issue = new PredictableSourceIssue("Predictable Source",
                    "The value of the API is not well randomized",
                    "",
                    "",
                    callSite, var.getName(),
                    predictableSourceRule.method().toString(),
                    callSite.getContainer().getSubsignature().toString());
        }
        else{
            issue = new PredictableSourceIssue("Predictable Source",
                    "The value of the API is not well randomized",
                    coi.allocation().toString(),
                    coi.sourceMethod().toString(),
                    callSite, var.getName(),
                    predictableSourceRule.method().toString(),
                    callSite.getContainer().getSubsignature().toString());
        }
        return issue;
    }
}
