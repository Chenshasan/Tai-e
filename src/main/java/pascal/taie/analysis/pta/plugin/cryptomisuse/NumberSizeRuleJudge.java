package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.NumberSizeRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class NumberSizeRuleJudge implements RuleJudge {

    NumberSizeRule numberSizeRule;

    CryptoObjManager manager;

    NumberSizeRuleJudge(NumberSizeRule numberSizeRule, CryptoObjManager manager) {
        this.numberSizeRule = numberSizeRule;
        this.manager = manager;
    }

    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, numberSizeRule.index());
        result.getPointsToSet(var).stream().
                filter(manager::isCryptoObj).
                forEach(cryptoObj -> {
                    if (cryptoObj.getAllocation() instanceof
                            CryptoObjInformation coi) {
                        int value = coi.constantValue() instanceof String ?
                                Integer.parseInt((String) coi.constantValue())
                                : (int) coi.constantValue();
                        if (value > numberSizeRule.max() ||
                                value < numberSizeRule.min()) {
                            match.set(false);
                        }
                    }
                });
        System.out.println("the result of number size in" + callSite + " is " + match.get());
        return match.get();
    }

}
