package pascal.taie.analysis.pta.plugin.cryptomisuse.compositeRule;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

public record FromSource(JMethod method, int index, Type type) {
}
