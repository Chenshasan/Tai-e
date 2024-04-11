package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.*;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.CompositeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.ToSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.CompositeRuleIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
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

    Logger logger = LogManager.getLogger(CompositeRuleJudge.class);

    public void addRuleJudge(Stmt stmt, RuleJudge ruleJudge) {
        this.RuleJudgeList.put(stmt, ruleJudge);
    }

    public CompositeRuleJudge(CompositeRule compositeRule, CryptoObjManager manager) {
        compositeRule.getToVarToStmtAndToSource().forEach((toVar, pair) -> {
            Stmt stmt = pair.first();
            ToSource toSource = pair.second();
            RuleJudge ruleJudge = null;
            if (toSource.rule() instanceof PatternMatchRule pm) {
                ruleJudge = new PatternMatchRuleJudge(pm, manager);
            } else if (toSource.rule() instanceof NumberSizeRule ns) {
                ruleJudge = new NumberSizeRuleJudge(ns, manager);
            } else if (toSource.rule() instanceof PredictableSourceRule ps) {
                ruleJudge = new PredictableSourceRuleJudge(ps, manager);
            } else {
            }
            assert ruleJudge != null;
            RuleJudgeList.put(stmt, ruleJudge);
        });
    }

    @Override
    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean judgeResult = new AtomicBoolean(true);
        CompositeRuleIssue compositeRuleIssue = new CompositeRuleIssue();
        RuleJudgeList.get(callSite).forEach(ruleJudge -> {
            Issue issue = ruleJudge.judge(result, callSite);
            if (ruleJudge instanceof PatternMatchRuleJudge) {
                // If the PatternMatchRuleJudge determines that this string does not match,
                // further processing will be halted.
                if (issue != null) {
                    compositeRuleIssue.setPredicate(1);
                }
            } else {
                if (issue != null) {
                    compositeRuleIssue.addIssue(issue);
                }
            }
        });
        report(judgeResult.get());
        return compositeRuleIssue;
    }

    public void report(boolean b) {
        logger.debug("Rule judge type: Composite Rule Judge"
                + "final result: " + b
                + "\n");
    }
}
