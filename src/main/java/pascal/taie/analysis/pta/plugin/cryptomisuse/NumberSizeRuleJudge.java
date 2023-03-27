package pascal.taie.analysis.pta.plugin.cryptomisuse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.NumberSizeRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class NumberSizeRuleJudge implements RuleJudge {

    NumberSizeRule numberSizeRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(NumberSizeRuleJudge.class);

    NumberSizeRuleJudge(NumberSizeRule numberSizeRule, CryptoObjManager manager) {
        this.numberSizeRule = numberSizeRule;
        this.manager = manager;
    }

    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, numberSizeRule.index());
        if(CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())){
            result.getPointsToSet(var).stream().
                    filter(manager::isCryptoObj).
                    forEach(cryptoObj -> {
                        if (cryptoObj.getAllocation() instanceof
                                CryptoObjInformation coi) {
                            int value = coi.constantValue() instanceof String ?
                                    Integer.parseInt((String) coi.constantValue())
                                    : (int) coi.constantValue();
                            if (value >= numberSizeRule.max() ||
                                    value < numberSizeRule.min()) {
                                match.set(false);
                                report(coi, var,callSite);
                            }
                        }
                    });
            System.out.println("the result of number size in" + callSite + " is " + match.get());
        }
        return match.get();
    }

    public void report(CryptoObjInformation coi, Var var, Invoke callSite) {
        logger.info("Rule judge type: Number Size"
                + "Message: The number size is not allowed for the API"
                + "Constant value: " + coi.constantValue()
                + "Var: " + var + "\n"
                + "Class: " + callSite.getContainer().getDeclaringClass() +"\n"
                + "Method: " + callSite.getContainer() +"\n"
                + "Call site: " + callSite + "\n"
                + "Source stmt: " + coi.allocation()
                + "\n");
    }

}
