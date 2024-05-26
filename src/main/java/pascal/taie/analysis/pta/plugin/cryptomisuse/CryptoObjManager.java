package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.CompositeRule;
import pascal.taie.language.type.IntType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class CryptoObjManager {
    private static final Descriptor CRYPTO_DESC = () -> "CryptoObj";

    private static final Descriptor PREDICT_DESC = () -> "PredictableObj";

    private static final Descriptor NUMBER_DESC = () -> "NumberObj";

    private static final Descriptor COMPOSITE_CRYPTO_DESC = () -> "CompositeCryptoObj";

    private final HeapModel heapModel;

    private final Map<Type, Obj> predictableObjs = Maps.newMap();

    private final Obj numberObj;

    CryptoObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
        numberObj = heapModel.getMockObj(NUMBER_DESC, "NumberObj", IntType.INT);
    }

    Obj makePredictableCryptoObj(Type type) {
        return predictableObjs.computeIfAbsent(type,
                t -> heapModel.getMockObj(PREDICT_DESC, "PredictableObj", t));
    }

    Obj makeNumberCryptoObj() {
        return numberObj;
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

    public boolean isPredictableCryptoObj(Obj obj){
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescriptor().equals(PREDICT_DESC);
    }

    public boolean isNumericCryptoObj(Obj obj){
        return  obj instanceof MockObj &&
                ((MockObj) obj).getDescriptor().equals(NUMBER_DESC);
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
