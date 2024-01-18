package pascal.taie.analysis.pta.plugin.cryptomisuse.issue;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.ArrayList;
import java.util.List;


public class IssueList implements Issue{

    private List<Issue> issues;

    public IssueList(List<Issue> issues) {
        this.issues = issues;
    }

    public IssueList(){
        this.issues = new ArrayList<>();
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void addIssue(Issue issue){
        issues.add(issue);
    }

}
