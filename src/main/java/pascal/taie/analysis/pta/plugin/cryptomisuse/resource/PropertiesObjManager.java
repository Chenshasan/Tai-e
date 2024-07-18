package pascal.taie.analysis.pta.plugin.cryptomisuse.resource;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjInformation;
import pascal.taie.language.type.IntType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import java.util.Properties;

public class PropertiesObjManager {

    private static final Descriptor PROPERTIES_DESC = () -> "PropertiesObj";

    private final HeapModel heapModel;

    PropertiesObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    Obj makePropObj(Properties properties, Type type) {
        return heapModel.getMockObj(PROPERTIES_DESC, properties, type);
    }

    public boolean isPropObj(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescriptor().equals(PROPERTIES_DESC);
    }

    public Properties getProperties(Obj obj) {
        if (isPropObj(obj)) {
            return (Properties) obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a prop object");
    }
}


