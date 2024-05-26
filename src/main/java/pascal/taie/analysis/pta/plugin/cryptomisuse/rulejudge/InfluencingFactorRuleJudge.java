package pascal.taie.analysis.pta.plugin.cryptomisuse.rulejudge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoAPIMisuseAnalysis;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjInformation;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjManager;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.CompositeRuleIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.InfluencingFactorIssue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.issue.Issue;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.InfluencingFactorRule;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InfluencingFactorRuleJudge implements RuleJudge {

    InfluencingFactorRule influencingFactorRule;

    CryptoObjManager manager;

    ClassHierarchy classHierarchy;

    Logger logger = LogManager.getLogger(NumberSizeRuleJudge.class);

    public InfluencingFactorRuleJudge(InfluencingFactorRule influencingFactorRule,
                                      CryptoObjManager manager,
                                      ClassHierarchy classHierarchy) {
        this.influencingFactorRule = influencingFactorRule;
        this.manager = manager;
        this.classHierarchy = classHierarchy;
    }

    public Issue judge(PointerAnalysisResult result, Invoke callSite) {
        String index = influencingFactorRule.index();
        String type = influencingFactorRule.type();
        Issue issue = null;
        Var var;
        JMethod container = influencingFactorRule.getMethod();
        Collection<Stmt> stmts = container.getIR().getStmts();
        if (!type.equals("exist")) {
            if (CryptoAPIMisuseAnalysis.getAppClasses().
                    contains(influencingFactorRule.method().getDeclaringClass())) {
                if (index.equals("result")) {
                    Collection<Var> returnVars = Sets.newSet();
                    stmts.forEach(stmt -> {
                        if (stmt instanceof Return returnStmt) {
                            returnVars.add(returnStmt.getValue());
                        }
                    });
                    if (returnVars.size() > 1) {
                        return null;
                    } else {
                        if (type.equals("def")) {
                            boolean influence = returnVars.stream().anyMatch(
                                    returnVar -> getDefStmtsNum(returnVar) > 1);
                            if (!influence) {
                                issue = report(null, null, null);
                            }
                        }
                    }
                } else if (index.equals("exception")) {
                    Collection<Var> exceptionVars = Sets.newSet();
                    boolean influence = false;
                    stmts.forEach(stmt -> {
                        if (stmt instanceof Throw throwStmt) {
                            exceptionVars.add(throwStmt.getExceptionRef());
                        }
                    });
                    if (exceptionVars.size() > 0) {
                        influence = exceptionVars.stream().anyMatch(
                                exceptionVar -> getUseStmtsNum(exceptionVar) > 1);
                    }
                    if (container.getExceptions().size() > 0) {
                        influence = true;
                    }
                    if (!influence) {
                        issue = report(null, null, null);
                    }
                } else if (index.matches("\\d+")) {
                    var = container.getIR().getParam(Integer.parseInt(index));
                    if (type.equals("use")) {
                        if (getUseStmtsNum(var) < 1) {
                            issue = report(null, var, null);
                        }
                    } else {
                        if (getDefStmtsNum(var) < 1) {
                            issue = report(null, var, null);
                        }
                    }
                }
            }
        } else {
//            logger.info("aaaaaaaaaaaaaa" + influencingFactorRule.index());
            List<String> indexes = parseString(influencingFactorRule.index());
            Set<String> needExist = Sets.newSet();
            Set<String> shouldNotExist = Sets.newSet();
            indexes.forEach(str -> {
                if (str.startsWith("!")) {
                    shouldNotExist.add(str.substring(1));
                } else {
                    needExist.add(str);
                }
            });
            Set<Invoke> invokeSet = result.getCallGraph().
                    getCallersOf(influencingFactorRule.method());
//            logger.info("aaaaaaaaaaaaaa" + influencingFactorRule.method());
            for (Invoke invoke : invokeSet) {
                JMethod method = invoke.getContainer();
                boolean need = needExist.stream().allMatch(str -> judgeExist(method, str, result));
                boolean shouldNot = shouldNotExist.stream().noneMatch(str -> judgeExist(method, str, result));
                if (need && shouldNot) {
                    issue = report(null, null, invoke);
                }
            }
        }
        return issue;
    }

    public Issue report(CryptoObjInformation coi, Var var, Invoke callSite) {
        String index = influencingFactorRule.index();
        JMethod method = influencingFactorRule.getMethod();
        InfluencingFactorIssue issue;
        if (index.equals("result")) {
            issue = new InfluencingFactorIssue("Influencing Factor",
                    "Should process the return value of this method instead of returning directly",
                    method.toString(),
                    method.getSubsignature().toString());
        } else if (index.equals("exception")) {
            issue = new InfluencingFactorIssue("Influencing Factor",
                    "Should throw an exception or produce some output " +
                            "based on the exception information in this method",
                    method.toString(),
                    method.getSubsignature().toString());
        } else if (index.matches("\\d+")) {
            issue = new InfluencingFactorIssue("Influencing Factor",
                    "Should " + influencingFactorRule.type() + " the var: " + var,
                    method.toString(),
                    method.getSubsignature().toString());
        } else {
            List<String> indexes = parseString(influencingFactorRule.index());
            String message = "";
            for (String str : indexes) {
                if (message.length() == 0) {
                    message = message + str;
                } else {
                    message = message + " and " + str;
                }
            }
            issue = new InfluencingFactorIssue("Influencing Factor",
                    "Should use all the invocation of " + message,
                    method.toString(),
                    method.getSubsignature().toString());
        }
        return issue;
    }

    private int getDefStmtsNum(Var var) {
        IR ir = influencingFactorRule.getMethod().getIR();
        AtomicInteger stmtsNum = new AtomicInteger();
        ir.getStmts().forEach(stmt -> {
            if (stmt.getDef().isPresent() && stmt.getDef().get() instanceof Var defVar) {
                if (defVar.getName().equals(var.getName())) {
                    stmtsNum.getAndIncrement();
                }
            }
        });
        stmtsNum.addAndGet(var.getStoreArrays().size());
        return stmtsNum.get();
    }

    private int getUseStmtsNum(Var var) {
        IR ir = influencingFactorRule.getMethod().getIR();
        AtomicInteger stmtsNum = new AtomicInteger();
        ir.getStmts().forEach(stmt -> {
            stmt.getUses().forEach(rValue -> {
                if (rValue instanceof Var useVar) {
                    if (useVar.getName().equals(var.getName())) {
                        stmtsNum.getAndIncrement();
                    }
                }
            });
        });
        stmtsNum.addAndGet(var.getStoreArrays().size());
        return stmtsNum.get();
    }

    private List<String> parseString(String input) {
        // 去除外层的方括号和单引号
        String trimmedInput = input.substring(1, input.length() - 1).replace("'", "");

        // 以逗号为分隔符进行拆分
        List<String> result = new ArrayList<>();
        int level = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : trimmedInput.toCharArray()) {
            if (c == ',' && level == 0) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                if (c == '<') level++;
                else if (c == '>') level--;
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result;
    }

    private boolean judgeExist(JMethod jMethod, String criterion, PointerAnalysisResult result) {
        boolean res = jMethod.getIR().getStmts().stream().anyMatch(stmt -> {
            if (criterion.startsWith("<")) {
                if (stmt instanceof Invoke invoke) {
                    return result.getCallGraph()
                            .getCalleesOf(invoke)
                            .stream()
                            .anyMatch(jMethod1 -> jMethod1.getSignature().contains(criterion));
                }
            } else {
                if (stmt instanceof Invoke invoke) {
                    if (invoke.getInvokeExp().toString().contains(criterion)) {
                        return true;
                    }
                }
            }
            return false;
        });
        return res;
    }
}
