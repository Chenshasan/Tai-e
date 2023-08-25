package pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule;

import pascal.taie.analysis.pta.plugin.cryptomisuse.CryptoObjPropagate;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import java.util.Map;
import java.util.Set;

public class CompositeRule implements Cloneable {
    FromSource fromSource;
    Var fromVar;
    Set<ToSource> toSourceSet;
    Set<CryptoObjPropagate> transfers;
    MultiMap<Var, Pair<Stmt, ToSource>> toVarToStmtAndToSource = Maps.newMultiMap();

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

    public MultiMap<Var, Pair<Stmt, ToSource>> getToVarToStmtAndToSource() {
        return toVarToStmtAndToSource;
    }


    public Var getFromVar() {
        return fromVar;
    }

    public void setFromVar(Var fromVar) {
        this.fromVar = fromVar;
    }

    public void refresh() {
        toVarToStmtAndToSource = Maps.newMultiMap();

    }

    @Override
    public CompositeRule clone() {
        try {
            CompositeRule compositeRule = (CompositeRule) super.clone();
            compositeRule.refresh();
            return compositeRule;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
