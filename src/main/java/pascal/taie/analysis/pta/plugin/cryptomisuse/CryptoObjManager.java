package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.CompositeRule;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

public class CryptoObjManager {
    private static final Descriptor CRYPTO_DESC = () -> "CryptoObj";

    private static final Descriptor COMPOSITE_CRYPTO_DESC = () -> "CompositeCryptoObj";

    private final HeapModel heapModel;

    CryptoObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    Obj makeCryptoObj(CryptoObjInformation source, Type type) {
        return heapModel.getMockObj(CRYPTO_DESC, source, type);
    }

    Obj makeCompositeCryptoObj(CompositeRule compositeRule, Type type) {
        return heapModel.getMockObj(COMPOSITE_CRYPTO_DESC, compositeRule, type);
    }

    /**
     * @return true if given obj represents a crypto API misuse object, otherwise false.
     */
    public boolean isCryptoObj(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescriptor().equals(CRYPTO_DESC);
    }

    public boolean isCompositeCryptoObj(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescriptor().equals(COMPOSITE_CRYPTO_DESC);
    }

    public boolean isCryptoInvolvedObj(Obj obj) {
        return isCryptoObj(obj) || isCompositeCryptoObj(obj);
    }


    public CryptoObjInformation getAllocationOfCOI(Obj obj) {
        if (isCryptoObj(obj)) {
            return (CryptoObjInformation) obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a crypto object");
    }

    public CompositeRule getAllocationOfRule(Obj obj) {
        if (isCompositeCryptoObj(obj)) {
            return (CompositeRule) obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a composite crypto object");
    }
}
