package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositeRule.CompositeRule;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

public class CryptoObjManager {
    private static final String CRYPTO_DESC = "CryptoObj";

    private static final String COMPOSITE_CRYPTO_DESC = "CompositeCryptoObj";

    private final HeapModel heapModel;

    CryptoObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    Obj makeCryptoObj(CryptoObjInformation source, Type type) {
        return heapModel.getMockObj(CRYPTO_DESC, source, type);
    }

    Obj makeCompositeCryptoObj(CompositeRule compositeRule,Type type){
        return heapModel.getMockObj(COMPOSITE_CRYPTO_DESC, compositeRule, type);
    }

    /**
     * @return true if given obj represents a crypto API misuse object, otherwise false.
     */
    boolean isCryptoObj(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescription().equals(CRYPTO_DESC);
    }

    boolean isCompositeCryptoObj(Obj obj){
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescription().equals(COMPOSITE_CRYPTO_DESC);
    }

    boolean isCryptoInvolvedObj(Obj obj){
        return isCryptoObj(obj) || isCompositeCryptoObj(obj);
    }


    CryptoObjInformation getAllocationOfCOI(Obj obj) {
        if (isCryptoObj(obj)) {
            return (CryptoObjInformation)obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a crypto object");
    }

    CompositeRule getAllocationOfRule(Obj obj){
        if (isCompositeCryptoObj(obj)) {
            return (CompositeRule)obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a composite crypto object");
    }
}
