package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import pascal.taie.ir.stmt.Invoke;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PredictableSourceIssue implements Issue {
    public PredictableSourceIssue(String judgeType, String message, String sourceStmt,
                                  String sourceMethod, Invoke callSite, String var,
                                  String calleeMethod, String subSignature) {
        this.judgeType = judgeType;
        this.message = message;
        this.sourceStmt = sourceStmt;
        this.sourceMethod = sourceMethod;
        this.callSite = callSite;
        this.var = var;
        this.calleeMethod = calleeMethod;
        this.subSignature = subSignature;
    }

    @JsonProperty("judgeType")
    private String judgeType;

    @JsonProperty("message")
    private String message;

    @JsonProperty("sourceStmt")
    private String sourceStmt;

    @JsonProperty("sourceMethod")
    private String sourceMethod;

    public Invoke getCallSite() {
        return callSite;
    }

    @JsonProperty("callSite")
    private Invoke callSite;

    @JsonProperty("var")
    private String var;

    @JsonProperty("calleeMethod")
    private String calleeMethod;
    @JsonProperty("subSignature")
    private String subSignature;

    public String getCalleeMethod(){
        return calleeMethod;
    }
}
