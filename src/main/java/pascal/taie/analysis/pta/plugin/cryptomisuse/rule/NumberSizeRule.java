package pascal.taie.analysis.pta.plugin.cryptomisuse.rule;

import pascal.taie.language.classes.JMethod;

public record NumberSizeRule(JMethod method, int index, int min, int max) implements Rule{
    @Override
    public JMethod getMethod() {
        return method;
    }
}
