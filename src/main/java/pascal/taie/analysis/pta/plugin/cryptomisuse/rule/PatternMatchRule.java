package pascal.taie.analysis.pta.plugin.cryptomisuse.rule;

import pascal.taie.language.classes.JMethod;

public record PatternMatchRule(JMethod method,int index,String pattern) implements Rule{
    @Override
    public JMethod getMethod() {
        return method;
    }
}
