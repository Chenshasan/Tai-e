package pascal.taie.analysis.pta.plugin.cryptomisuse.rule;

import pascal.taie.language.classes.JMethod;

public record CoOccurrenceRule(JMethod method, String index) implements Rule {
    @Override
    public JMethod getMethod() {
        return method;
    }
}
