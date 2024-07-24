package pascal.taie.analysis.pta.plugin.cryptomisuse.resource;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjInformation;
import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjManager;
import pascal.taie.analysis.pta.plugin.util.AnalysisModelPlugin;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

public class ResourceRetrieverModel extends AnalysisModelPlugin {

    private static Path classpath;

    private PropertiesObjManager propObjManager;

    private CryptoObjManager cryptoObjManager;

    private static final Descriptor PROPERTIES_DESC = () -> "PropertiesObj";

    public static void setClasspath(Path classpath) {
        ResourceRetrieverModel.classpath = classpath;
    }

    public ResourceRetrieverModel(Solver solver, HeapModel heapModel, CryptoObjManager cryptoObjManager) {
        super(solver);
        this.propObjManager = new PropertiesObjManager(heapModel);
        this.cryptoObjManager = cryptoObjManager;
    }

    @InvokeHandler(signature = "<java.util.Properties: void load(java.io.InputStream)>", argIndexes = {BASE})
    public void loadResource(Context context, Invoke invoke, PointsToSet base) {
        // arg0 is an inputStream var
        Var arg0 = invoke.getInvokeExp().getArg(0);
        invoke.getContainer().getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof Invoke inputStreamGetter
                    && inputStreamGetter.getDef().isPresent()
                    && arg0.equals(inputStreamGetter.getDef().get())
                    && inputStreamGetter.getInvokeExp().getMethodRef().
                    toString().contains("getResourceAsStream")) {

                // inputStreamGetter is the stmt of get properties
                Var getterArg0 = inputStreamGetter.getInvokeExp().getArg(0);
                if (getterArg0.isConst() && getterArg0.getConstValue() instanceof StringLiteral stringLiteral) {
                    String propertiesFile = stringLiteral.getString();
                    try {
                        Properties properties = readPropertiesFromClasspath(classpath, propertiesFile);
                        Var baseVar = ((InvokeInstanceExp) invoke.getInvokeExp()).getBase();

                        //add properties into base var
                        solver.addVarPointsTo(context, baseVar,
                                propObjManager.makePropObj(properties, baseVar.getType()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @InvokeHandler(signature = "<java.util.Properties: java.lang.String getProperty(java.lang.String,java.lang.String)>", argIndexes = {BASE})
    public void retrieveResource(Context context, Invoke invoke, PointsToSet pts) {
        pts.forEach(csObj -> {
            Obj obj = csObj.getObject();
            if (propObjManager.isPropObj(obj)) {
                Properties properties = propObjManager.getProperties(obj);
                Var arg0 = invoke.getInvokeExp().getArg(0);
                if (arg0.isConst() && arg0.getConstValue() instanceof StringLiteral stringLiteral) {
                    String key = stringLiteral.getString();
                    String value = properties.getProperty(key);
                    if (value != null) {
                        CryptoObjInformation coi =
                                new CryptoObjInformation(invoke, invoke.getContainer(), value);
                        Obj cryptoObj = cryptoObjManager.makeCryptoObj(coi, invoke.getLValue().getType());
                        solver.addVarPointsTo(context, invoke.getLValue(), cryptoObj);
                    }
                    else{
                        Var arg1 = invoke.getInvokeExp().getArg(1);
                        if (arg1.isConst() && arg1.getConstValue() instanceof StringLiteral defStringLiteral) {
                            String defaultValue = defStringLiteral.getString();
                            CryptoObjInformation coi =
                                    new CryptoObjInformation(invoke, invoke.getContainer(), defaultValue);
                            Obj cryptoObj = cryptoObjManager.makeCryptoObj(coi, invoke.getLValue().getType());
                            solver.addVarPointsTo(context, invoke.getLValue(), cryptoObj);
                        }
                    }
                }
            }
        });
    }


    public static Properties readPropertiesFromClasspath(Path classpath, String propertiesFile) throws IOException {
        // 遍历文件夹查找指定的 .properties 文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(classpath, "*.properties")) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().equals(propertiesFile)) {
                    // 读取 .properties 文件内容
                    try (InputStream inputStream = Files.newInputStream(entry)) {
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        return properties;
                    }
                }
            }
        }
        return null;
    }
}
