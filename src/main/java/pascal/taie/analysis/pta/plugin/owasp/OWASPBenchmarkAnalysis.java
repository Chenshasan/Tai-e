package pascal.taie.analysis.pta.plugin.owasp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.spring.AppFirstParamProvider;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;

public class OWASPBenchmarkAnalysis implements Plugin {
    private static final Logger logger = LogManager.getLogger(OWASPBenchmarkAnalysis.class);

    private Solver solver;

    private ClassHierarchy classHierarchy;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.classHierarchy = solver.getHierarchy();
    }

    @Override
    public void onStart() {
        JClass objectClass=classHierarchy.getClass("java.lang.Object");
        classHierarchy.getAllSubclassesOf(objectClass).forEach(jClass -> {
            if(jClass.getName().contains("BenchmarkTest")){
                jClass.getDeclaredMethods().forEach(jMethod -> {
                    if(jMethod.getSignature().contains("doPost")){
                        SpecifiedParamProvider.Builder builder = new SpecifiedParamProvider.Builder(jMethod)
                                .setDelegate(new OWASPParamProvider());
                        SpecifiedParamProvider paramProvider = builder.build();
                        solver.addEntryPoint(new EntryPoint(jMethod, paramProvider));
                        logger.info("""
                            [OWASP Analysis] Adding entry point
                            \tmethod: {}
                            \tparamProvider: {}
                            """, jMethod, paramProvider);
                    }
                });
            }
        });
    }
}
