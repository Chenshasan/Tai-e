package pascal.taie.analysis.pta.plugin.cryptomisuse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PatternMatchRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

public class PatternMatchRuleJudge implements RuleJudge {

    PatternMatchRule patternMatchRule;

    CryptoObjManager manager;

    PatternMatchRuleJudge(PatternMatchRule patternMatchRule, CryptoObjManager manager) {
        this.patternMatchRule = patternMatchRule;
        this.manager = manager;
    }

    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, patternMatchRule.index());
        result.getPointsToSet(var).stream().
                filter(manager::isCryptoObj).
                forEach(cryptoObj -> {
                    if (cryptoObj.getAllocation() instanceof
                            CryptoObjInformation cryptoObjInformation) {
                        String value = (String) cryptoObjInformation.constantValue();
                        match.set(match.get() &&
                                (Pattern.matches(patternMatchRule.pattern(), value)));
                    }
                });
        System.out.println("the result of " + callSite + " is " + match.get());
        return match.get();
    }
}
