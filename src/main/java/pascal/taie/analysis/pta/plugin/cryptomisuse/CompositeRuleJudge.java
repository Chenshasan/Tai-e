package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositeRule.CompositeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.NumberSizeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PatternMatchRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PredictableSourceRule;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.concurrent.atomic.AtomicBoolean;


public class CompositeRuleJudge implements RuleJudge {

    private final MultiMap<Stmt, RuleJudge> RuleJudgeList = Maps.newMultiMap();

    public void addRuleJudge(Stmt stmt, RuleJudge ruleJudge) {
        this.RuleJudgeList.put(stmt, ruleJudge);
    }

    CompositeRuleJudge(CompositeRule compositeRule, CryptoObjManager manager) {
        compositeRule.getJudgeStmts().forEach((stmt, toSource) -> {
            RuleJudge ruleJudge = null;
            if (toSource.rule() instanceof PatternMatchRule pm) {
                ruleJudge = new PatternMatchRuleJudge(pm, manager);
            } else if (toSource.rule() instanceof NumberSizeRule ns) {
                ruleJudge = new NumberSizeRuleJudge(ns, manager);
            } else if (toSource.rule() instanceof PredictableSourceRule ps) {
                ruleJudge = new PredictableSourceRuleJudge(ps, manager);
            } else {
                //System.out.println("composite rule of var:" +compositeRule.getFromVar() + "is not appropriate");
            }
            assert ruleJudge != null;
            RuleJudgeList.put(stmt, ruleJudge);
        });
    }

    @Override
    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean judgeResult = new AtomicBoolean(true);
        RuleJudgeList.get(callSite).forEach(ruleJudge -> {
            judgeResult.set(judgeResult.get() && ruleJudge.judge(result, callSite));
        });
        return judgeResult.get();
    }
}
