package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompositeRuleIssue implements Issue {

    @JsonProperty("judgeType")
    private String judgeType = "CompositeRule";

    @JsonProperty("issues")
    private List<Issue> issues = new ArrayList<>();

    public Collection<Issue> getIssues() {
        return issues;
    }

    public void addIssue(Issue issue) {
        if(issue instanceof CompositeRuleIssue compositeRuleIssue){
            issues.addAll(compositeRuleIssue.getIssues());
        }
        else{
            issues.add(issue);
        }
    }
}
