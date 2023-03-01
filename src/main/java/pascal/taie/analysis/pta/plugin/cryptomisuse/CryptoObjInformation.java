package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.ir.stmt.Stmt;

public record CryptoObjInformation(Stmt allocation,Object constantValue) {

}
