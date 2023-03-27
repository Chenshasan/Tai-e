package pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule;

import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.Rule;
import pascal.taie.language.classes.JMethod;

public record ToSource(JMethod method, int index, Rule rule) {
}
