package pascal.taie.analysis.pta.plugin.spring.mapstruct;

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractIRModel;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

import java.util.List;
import java.util.function.Predicate;

public class MapStructModel extends AbstractIRModel {

    public MapStructModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerIRGens() {
        JMethod getMapper = hierarchy.getMethod(
                "<org.mapstruct.factory.Mappers: java.lang.Object getMapper(java.lang.Class)>");
        if (getMapper != null) {
            registerIRGen(getMapper, this::getMapper);
        }
    }

    private List<Stmt> getMapper(Invoke invoke) {
        Var arg0 = invoke.getInvokeExp().getArg(0);
        if (arg0.isConst() && arg0.getConstValue() instanceof ClassLiteral classLiteral
                && classLiteral.getTypeValue() instanceof ClassType classType) {
            return hierarchy.getAllSubclassesOf(classType.getJClass())
                    .stream()
                    .filter(Predicate.not(JClass::isAbstract))
                    .filter(JClass::isApplication)
                    .map(c -> (Stmt) new New(invoke.getContainer(), invoke.getLValue(),
                            new NewInstance(c.getType())))
                    .toList();
        }
        return List.of();
    }
}
