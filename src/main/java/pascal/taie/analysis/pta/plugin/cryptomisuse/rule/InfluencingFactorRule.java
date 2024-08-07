package pascal.taie.analysis.pta.plugin.cryptomisuse.rule;

import pascal.taie.language.classes.JMethod;

public record InfluencingFactorRule(JMethod method, String index, String type, String factor) implements Rule {
    @Override
    public JMethod getMethod() {
        return method;
    }
}
