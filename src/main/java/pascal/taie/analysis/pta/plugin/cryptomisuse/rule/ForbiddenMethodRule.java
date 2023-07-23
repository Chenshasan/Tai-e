package pascal.taie.analysis.pta.plugin.cryptomisuse.rule;

import pascal.taie.language.classes.JMethod;

public record ForbiddenMethodRule(JMethod method) implements Rule {

    @Override
    public JMethod getMethod() {
        return method;
    }
}
