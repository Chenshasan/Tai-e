package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.util.concurrent.atomic.AtomicBoolean;

public class PredictableSourceRuleJudge implements RuleJudge {

    private final String PREDICTABLE_DESC = "PredictableSourceObj";

    PredictableSourceRule predictableSourceRule;

    CryptoObjManager manager;

    PredictableSourceRuleJudge(PredictableSourceRule predictableSourceRule,
                               CryptoObjManager manager) {
        this.predictableSourceRule = predictableSourceRule;
        this.manager = manager;
    }

    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, predictableSourceRule.index());
        result.getPointsToSet(var).
                stream().
                filter(manager::isCryptoObj).
                forEach(cryptoObj -> {
                    if (cryptoObj.getAllocation() instanceof
                            CryptoObjInformation cryptoObjInformation) {
                        String desc = (String) cryptoObjInformation.constantValue();
                        if (desc.equals(PREDICTABLE_DESC)) {
                            match.set(false);
                        }
                    }
                });
        System.out.println("-----------------------predictable source result is " + match);
        return match.get();
    }
}
