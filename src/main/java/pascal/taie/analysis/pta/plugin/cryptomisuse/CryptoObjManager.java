package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

public class CryptoObjManager {
    private static final String CRYPTO_DESC = "CryptoObj";

    private final HeapModel heapModel;

    CryptoObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    Obj makeCryptoObj(CryptoObjInformation source, Type type) {
        return heapModel.getMockObj(CRYPTO_DESC, source, type);
    }

    /**
     * @return true if given obj represents a crypto API misuse object, otherwise false.
     */
    boolean isCryptoObj(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescription().equals(CRYPTO_DESC);
    }

    Object getAllocation(Obj obj) {
        if (isCryptoObj(obj)) {
            return obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a taint object");
    }


}
