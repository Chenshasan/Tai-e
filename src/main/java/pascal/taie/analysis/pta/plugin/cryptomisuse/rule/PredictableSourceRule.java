package pascal.taie.analysis.pta.plugin.cryptomisuse.rule;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

public record PredictableSourceRule(JMethod method, int index) implements Rule{
    @Override
    public JMethod getMethod() {
        return method;
    }
}
