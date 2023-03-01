package pascal.taie.analysis.pta.plugin.cryptomisuse;


import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

public record CryptoSource(JMethod method, Type type, int index) {

}
