package pascal.taie.analysis.pta.plugin.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.VirtualPointer;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * @see BeanManager
 */
public class Bean {

    private static final Logger logger = LogManager.getLogger(Bean.class);

    private final JClass jClass;

    private final Pointer pointer;

    /**
     * constructor of {@link Bean#jClass} <br>
     * or @Bean method whose return type is {@link Bean#jClass}
     */
    private final JMethod constructor;

    /**
     * In singleton mode, the obj is with empty context. <br>
     * So only one.
     */
    private final Obj obj;

    /**
     * some in-edges of {@link pascal.taie.analysis.pta.core.solver.PointerFlowGraph}
     */
    private final Collection<Var> ins = Sets.newSet();

    /**
     * some out-edges of {@link pascal.taie.analysis.pta.core.solver.PointerFlowGraph}
     */
    private final Collection<Var> outs = Sets.newSet();

    private final Collection<JField> outFields = Sets.newSet();

    public Bean(JClass jClass,
                @Nullable JMethod constructor,
                Obj obj) {
        this.jClass = jClass;
        this.constructor = constructor;
        this.pointer = new VirtualPointer(this, jClass.getType());
        this.obj = obj;
        Optional.ofNullable(constructor)
                .map(JMethod::getIR)
                .map(IR::getThis)
                .ifPresent(this::addOutEdge);
        logger.info("""
                        [Bean Manager] Create bean:
                        \tclass: {}
                        \tconstructor: {}
                        \tpointer: {}
                        \tobj: {}""",
                this.jClass, this.constructor, this.pointer, this.obj);
    }

    public Bean(JClass jClass, Collection<Var> inVars) {
        this.jClass = jClass;
        this.constructor = null;
        this.pointer = new VirtualPointer(this, jClass.getType());
        this.obj = null;
        inVars.forEach(this::addInEdge);
        logger.info("""
                        [Bean Manager] Create bean:
                        \tclass: {}
                        \tconstructor: {}
                        \tpointer: {}
                        \tobj: {}""",
                this.jClass, this.constructor, this.pointer, this.obj);
    }

    public Collection<JField> getOutFields() {
        return outFields;
    }

    public void addInEdge(Var in) {
        this.ins.add(in);
    }

    public void addOutEdge(Var out) {
        this.outs.add(out);
    }

    public void addOutEdge(JField out) {
        this.outFields.add(out);
    }

    public Pointer getPointer() {
        return this.pointer;
    }

    @Nullable
    public Obj getObj() {
        return this.obj;
    }

    public JClass getJClass() {
        return this.jClass;
    }

    @Nullable
    public JMethod getConstructor() {
        return constructor;
    }

    public Collection<Var> getIns() {
        return ins;
    }

    public Collection<Var> getOuts() {
        return outs;
    }
}
