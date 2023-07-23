package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import java.util.ArrayList;
import java.util.List;

public class CompositeRuleIssue implements Issue {

    private String judgeType = "CompositeRule";
    private List<Issue> issues = new ArrayList<>();

    public void addIssue(Issue issue) {
        issues.add(issue);
    }
}
