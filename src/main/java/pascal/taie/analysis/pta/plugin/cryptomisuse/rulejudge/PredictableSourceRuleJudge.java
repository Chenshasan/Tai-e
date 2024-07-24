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
        Var var = IndexUtils.getVar(callSite, predictableSourceRule.index());
        AtomicReference<Issue> issue = new AtomicReference<>();

        if (CryptoAPIMisuseAnalysis.getAppClasses().contains(
                callSite.getContainer().getDeclaringClass())) {
            boolean found = result.getPointsToSet(var).stream()
                    .filter(manager::isCryptoObj)
                    .anyMatch(cryptoObj -> {
                        if (cryptoObj.getAllocation() instanceof CryptoObjInformation coi) {
                            issue.set(report(coi, var, callSite));
                            if (coi.constantValue() instanceof String str && str.equals(PREDICTABLE_DESC)) {
                                return true;
                            }
                        }
                        return false;
                    });

            if (!found && result.getPointsToSet(var).stream().anyMatch(manager::isPredictableCryptoObj)) {
                issue.set(report(null, var, callSite));
            }
        }

        return issue.get();
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        String allocation = coi != null ? coi.allocation().toString() : "";
        String sourceMethod = coi != null ? coi.sourceMethod().toString() : "";

        return new PredictableSourceIssue(
                "Predictable Source",
                "The value of the API is not well randomized",
                allocation,
                sourceMethod,
                callSite,
                var.getName(),
                predictableSourceRule.method().toString(),
                callSite.getContainer().getSubsignature().toString()
        );
    }
}
