package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoOccurrenceRuleIssue implements Issue{

    public CoOccurrenceRuleIssue(String judgeType, String message,
                                  String calleeMethod, String subSignature) {
        this.judgeType = judgeType;
        this.message = message;
        this.calleeMethod = calleeMethod;
        this.subSignature = subSignature;
    }

    @JsonProperty("judgeType")
    private String judgeType;

    @JsonProperty("message")
    private String message;

    @JsonProperty("calleeMethod")
    private String calleeMethod;

    @JsonProperty("subSignature")
    private String subSignature;
}
