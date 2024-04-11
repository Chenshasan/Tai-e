package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.*;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.NumberSizeIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.NumberSizeRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NumberSizeRuleJudge implements RuleJudge {

    NumberSizeRule numberSizeRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(NumberSizeRuleJudge.class);

    public NumberSizeRuleJudge(NumberSizeRule numberSizeRule, CryptoObjManager manager) {
        this.numberSizeRule = numberSizeRule;
        this.manager = manager;
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, numberSizeRule.index());
        AtomicReference<Issue> issue = new AtomicReference<>();
        if (CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())) {
            result.getPointsToSet(var).stream().
                    filter(manager::isCryptoObj).
                    forEach(cryptoObj -> { 
                        if (cryptoObj.getAllocation() instanceof
                                CryptoObjInformation coi) {
                            logger.debug("var is " + var + "in callsite " + callSite);
                            if(isNumeric(coi.constantValue().toString())){
                                int value = coi.constantValue() instanceof String ?
                                        Integer.parseInt((String) coi.constantValue())
                                        : (int) coi.constantValue();
                                if (value >= numberSizeRule.max() ||
                                        value < numberSizeRule.min()) {
                                    match.set(false);
                                    issue.set(report(coi, var, callSite));
                                }
                            }
                        }
                    });
            logger.debug("the result of number size in" + callSite + " is " + match.get());
        }
        return issue.get();
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        NumberSizeIssue issue = new NumberSizeIssue("Number Size",
                "The number size is not allowed for the API",
                coi.allocation().toString(),
                coi.sourceMethod().toString(),
                callSite.toString(), var.getName(),
                coi.constantValue().toString(), numberSizeRule.method().toString(),
                numberSizeRule.min() + "-" + numberSizeRule.max(),
                callSite.getContainer().getSubsignature().toString());
        return issue;
    }

    public static boolean isNumeric(String str) {
        // 使用正则表达式判断是否只包含数字
        return str.matches("\\d+");
    }
}
