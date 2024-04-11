package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

public record CryptoObjInformation(Stmt allocation, JMethod sourceMethod, Object constantValue) {

}
