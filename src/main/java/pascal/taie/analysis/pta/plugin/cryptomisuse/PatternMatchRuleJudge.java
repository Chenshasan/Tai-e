package pascal.taie.analysis.pta.plugin.cryptomisuse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.PatternMatchRule;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;

public class PatternMatchRuleJudge implements RuleJudge {

    PatternMatchRule patternMatchRule;

    CryptoObjManager manager;

    Logger logger = LogManager.getLogger(PatternMatchRuleJudge.class);

    PatternMatchRuleJudge(PatternMatchRule patternMatchRule, CryptoObjManager manager) {
        this.patternMatchRule = patternMatchRule;
        this.manager = manager;
    }

    public boolean judge(PointerAnalysisResult result, Invoke callSite) {
        AtomicBoolean match = new AtomicBoolean(true);
        Var var = IndexUtils.getVar(callSite, patternMatchRule.index());
        if(CryptoAPIMisuseAnalysis.getAppClasses().
                contains(callSite.getContainer().getDeclaringClass())){
            result.getPointsToSet(var).stream().
                    filter(manager::isCryptoObj).
                    forEach(cryptoObj -> {
                        if (cryptoObj.getAllocation() instanceof
                                CryptoObjInformation coi) {
                            String value = (String) coi.constantValue();
                            logger.debug("coi constant value is " + value);
                            report(coi, var, callSite);
                            match.set(match.get() && !(Pattern.matches(
                                    patternMatchRule.pattern(), value)));
                        }
                    });
            logger.debug("the result of " + callSite + " is " + match.get());
        }
        return match.get();
    }

    public void report(CryptoObjInformation coi, Var var, Invoke callSite) {
        Stmt stmt = coi.allocation();
        logger.info("Rule judge type: Pattern Match "
                + "Message: The pattern is not matched for the API "
                + "Constant value: " + coi.constantValue() + "\n"
                + "Var: " + var + "\n"
                + "Class: " + callSite.getContainer().getDeclaringClass() + "\n"
                + "Method: " + callSite.getContainer() + "\n"
                + "Call site: " + callSite + "\n"
                + "Source stmt: " + stmt + "\n");
    }
}
