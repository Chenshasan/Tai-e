package pascal.taie.analysis.pta.plugin.cryptomisuse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

import java.util.concurrent.atomic.AtomicBoolean;

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

    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, predictableSourceRule.index());
        if(CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())){
            result.getPointsToSet(var).
                    stream().
                    filter(manager::isCryptoObj).
                    forEach(cryptoObj -> {
                        if (cryptoObj.getAllocation() instanceof
                                CryptoObjInformation coi) {
                            //String desc = (String) coi.constantValue();
                            if (match.get()) {
                                report(coi, var,callSite);
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
        return match.get();
    }

    public void report(CryptoObjInformation coi, Var var, Invoke callSite) {
        logger.info("Rule judge type: Predictable Source "
                + "Message: The value of the API is not well randomized "
                + "Var: " + var + "\n"
                + "Class: " + callSite.getContainer().getDeclaringClass() +"\n"
                + "Method: " + callSite.getContainer() +"\n"
                + "Call site: " + callSite + "\n"
                + "Source stmt: " + coi.allocation()
                + "\n");
    }
}
