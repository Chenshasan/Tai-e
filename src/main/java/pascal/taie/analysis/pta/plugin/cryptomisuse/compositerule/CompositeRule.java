package pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule;

import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjPropagate;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Map;
import java.util.Set;

public class CompositeRule implements Cloneable {
    FromSource fromSource;
    Var fromVar;
    Set<ToSource> toSourceSet;
    Set<CryptoObjPropagate> transfers;
    MultiMap<Stmt, ToSource> judgeStmts = Maps.newMultiMap();
    Map<Var, Stmt> toVarToStmt = Maps.newMap();
    Map<Var, ToSource> toSourceToToVar = Maps.newMap();

    public CompositeRule(FromSource fromSource,
                         Set<ToSource> toSourceSet,
                         Set<CryptoObjPropagate> transfers) {
        this.fromSource = fromSource;
        this.toSourceSet = toSourceSet;
        this.transfers = transfers;
    }

    public FromSource getFromSource() {
        return fromSource;
    }

    public Set<ToSource> getToSources() {
        return toSourceSet;
    }

    public Set<CryptoObjPropagate> getTransfers() {
        return transfers;
    }

    public MultiMap<Stmt, ToSource> getJudgeStmts() {
        return judgeStmts;
    }

    public Map<Var, Stmt> getToVarToStmt() {
        return toVarToStmt;
    }

    public Map<Var, ToSource> getToSourceToToVar() {
        return toSourceToToVar;
    }

    public Var getFromVar() {
        return fromVar;
    }

    public void setFromVar(Var fromVar) {
        this.fromVar = fromVar;
    }

    public void renewJudgeStmts() {
        this.judgeStmts = Maps.newMultiMap();
    }

    @Override
    public CompositeRule clone() {
        try {
            CompositeRule compositeRule = (CompositeRule)super.clone();
            compositeRule.renewJudgeStmts();
            return compositeRule;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
