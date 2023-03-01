package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.Rule;
import pascal.taie.ir.stmt.Invoke;

public interface RuleJudge {

    default boolean judge(PointerAnalysisResult result, Invoke callSite) {
        return true;
    }

    default void report(){
    }

}
