package pascal.taie.analysis.pta.plugin.cryptomisuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.PredictableSourceIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PredictableSourceRuleJudge implements RuleJudge {

    private final String PREDICTABLE_DESC = "PredictableSourceObj";

    PredictableSourceRule predictableSourceRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(PredictableSourceRuleJudge.class);

    PredictableSourceRuleJudge(PredictableSourceRule predictableSourceRule,
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
                        if (cryptoObj.getAllocation() instanceof
                                CryptoObjInformation coi) {
                            //String desc = (String) coi.constantValue();
                            if (match.get()) {
                                issue.set(report(coi, var, callSite));
                                logger.debug("the result of " + callSite
                                        + " of var: " + var
                                        + " is false"
                                        + " with crypto obj in"
                                        + ((CryptoObjInformation) cryptoObj.getAllocation()).allocation());
                            }
                            match.set(false);
                        }
                    });
        }
        return issue.get();
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        PredictableSourceIssue issue = new PredictableSourceIssue("Predictable Source",
                "The value of the API is not well randomized",
                coi.allocation().toString(),
                coi.sourceMethod().toString(),
                callSite.toString(), var.getName(),
                predictableSourceRule.method().toString(),
                callSite.getContainer().getSubsignature().toString());
        return issue;
    }
}
