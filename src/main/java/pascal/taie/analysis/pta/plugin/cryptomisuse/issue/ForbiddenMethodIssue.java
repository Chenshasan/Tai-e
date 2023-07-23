package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForbiddenMethodIssue implements Issue {

    public ForbiddenMethodIssue(String judgeType, String message, String callSite,
                                String calleeMethod, String subSignature) {
        this.judgeType = judgeType;
        this.message = message;
        this.callSite = callSite;
        this.calleeMethod = calleeMethod;
        this.subSignature = subSignature;
    }

    @JsonProperty("judgeType")
    private String judgeType;

    @JsonProperty("message")
    private String message;

    @JsonProperty("callSite")
    private String callSite;

    @JsonProperty("calleeMethod")
    private String calleeMethod;

    @JsonProperty("subSignature")
    private String subSignature;
}
