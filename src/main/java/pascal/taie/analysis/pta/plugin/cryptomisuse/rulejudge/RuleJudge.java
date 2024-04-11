package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.Rule;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

public interface RuleJudge {

    default Issue judge(PointerAnalysisResult result, Invoke callSite) {
        return null;
    }

    default void report(){
    }

}
