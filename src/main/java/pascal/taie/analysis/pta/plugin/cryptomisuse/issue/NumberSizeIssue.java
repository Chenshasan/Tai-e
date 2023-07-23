package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NumberSizeIssue implements Issue {

    public NumberSizeIssue(String judgeType, String message, String sourceStmt,
                           String sourceMethod, String callSite, String var,
                           String constantValue, String calleeMethod,
                           String numberSize, String subSignature) {
        this.judgeType = judgeType;
        this.message = message;
        this.sourceStmt = sourceStmt;
        this.sourceMethod = sourceMethod;
        this.callSite = callSite;
        this.var = var;
        this.constantValue = constantValue;
        this.calleeMethod = calleeMethod;
        this.numberSize = numberSize;
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

    @JsonProperty("callSite")
    private String callSite;

    @JsonProperty("var")
    private String var;

    @JsonProperty("constantValue")
    private String constantValue;

    @JsonProperty("calleeMethod")
    private String calleeMethod;

    @JsonProperty("numberSize")
    private String numberSize;
    @JsonProperty("subSignature")
    private String subSignature;

}
