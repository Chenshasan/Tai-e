package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.BytecodeBlock;
import pascal.taie.frontend.newfrontend.DUInfo;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.IVarManager;
import pascal.taie.frontend.newfrontend.Lenses;
import pascal.taie.frontend.newfrontend.SparseSet;
import pascal.taie.frontend.newfrontend.StmtVarVisitor;
import pascal.taie.frontend.newfrontend.VarManager;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.TriFunction;
import pascal.taie.util.collection.IndexMap;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * <p>Cytron style SSA transformation.</p>
 * <p>See "Efficiently Computing Static Single Assignment
 * Form and the Control Dependence Graph"
 * by Ron Cytron, Jeanne Ferrante, Barry K. Rosen, Mark N. Wegman, and F. Kenneth Zadeck
 * for details.</p>
 *
 * <p>This pass may be used to transform TIR generated by java sources.
 * Thus, generic basic block / varManger is used</p>
 */
public class SSATransform<Block extends IBasicBlock> {
    private final JMethod method;

    private final IndexedGraph<Block> graph;

    private final Dominator.DominatorFrontiers df;

    private final int[] dom;

    private final int[] postOrder;

    private final IVarManager manager;

    private final List<Var> vars;

    private final DUInfo info;

    private final boolean hasExceptionHandler; // Only for debug. Remove when released.

    private final boolean DEBUG = true;

    public SSATransform(
            JMethod method,
            IndexedGraph<Block> graph,
            IVarManager manager,
            DUInfo info,
            boolean hasExceptionHandler
    ) {
        this.method = method;
        this.graph = graph;
        Dominator<Block> dominator = new Dominator<>(graph);
        this.df = dominator.getDF();
        this.dom = dominator.getDomTree();
        this.postOrder = dominator.getPostOrder();
        this.manager = manager;
        this.vars = manager.getVars();
        this.info = info;
        this.hasExceptionHandler = hasExceptionHandler;
    }

    public void build() {
        // step 1. insert phi functions
        phiInsertion();
        // step 1.5. initiate some data structures for efficient pruning
        prepareForPruning();
        // step 2. rename variables
        renaming();
        // step 3. remove dead phi functions
        pruning();
        // step 4. resume the name of the variables from the VarTable if source is ASM
        if (manager instanceof VarManager) {
            resumeNames();
        }

        if (DEBUG) {
            // testify whether step 1.5 creates those data structures that is big enough
            assert safeEstimationForPhiVars >= vars.size();
            if (hasExceptionHandler) return; // currently exception is not handled correctly
            // testify pruning.
            for (int i = 0; i < graph.size(); i++) {
                for (Stmt stmt : graph.getNode(i).getStmts()) {
                    if (stmt instanceof PhiStmt p) {
                        boolean useless = isUseless.has(p.getLValue().getIndex());
                        boolean renamed = p.getBase() != p.getLValue();
                        boolean undead = p.getRValue().getUsesAndInBlocks().size() > 1;
                        assert !useless && (renamed && undead);
                    }
                }
            }
        }

        // step 5. remove the pruned variables that are defined by the pruned phi stmts.
        removePrunedVars();
    }

    private List<Block> inEdges(int i) {
        return graph.inEdges(graph.getNode(i));
    }

    private final List<IndexerBitSet<Var>> isInserted = new ArrayList<>();
    private final List<List<Stmt>> phis = new ArrayList<>();
    private Var[] nonSSAVars;

    private final Indexer<Var> indexer = new Indexer<>() {
        @Override
        public int getIndex(Var o) {
            if (o == null) return -1;
            return o.getIndex();
        }

        @Override
        public Var getObject(int index) {
            return vars.get(index);
        }
    };

    private void phiInsertion() {
        // index with block.getIndex()
        nonSSAVars = manager.getNonSSAVar();
        for (int i = 0; i < graph.size(); i++) {
            // TODO: current implement cannot set the init size
            //       change it if it's a performance issue
            int entry = graph.getIntEntry();
            if (inEdges(i).size() >= 2 || (i == entry && !inEdges(entry).isEmpty())) {
                // only joint point needs phi functions
                isInserted.add(new IndexerBitSet<>(indexer, false));
                phis.add(new ArrayList<>());
            } else {
                isInserted.add(null);
                phis.add(null);
            }
            // collect total stmtCount for step 1.5
            stmtCount += graph.getNode(i).getStmts().size();
        }

        SparseSet current = new SparseSet(graph.size(), graph.size());
        for (Var v : nonSSAVars) {
            if (!isVarToBeSSA(v)) continue;
            Set<IBasicBlock> defBlocks = info.getDefBlock(v);
            for (IBasicBlock block : defBlocks) {
                current.add(block.getIndex());
            }
            while (!current.isEmpty()) {
                IBasicBlock block = graph.getNode(current.removeLast());
                for (int node : df.get(block.getIndex())) {
                    IBasicBlock dfb = graph.getNode(node);
                    Stmt stmt = !dfb.getStmts().isEmpty() ? dfb.getStmts().get(0) : null;
                    Catch potentialCatch = stmt instanceof Catch c ? c : null;
                    if (potentialCatch != null && potentialCatch.getExceptionRef() == v) {
                        continue;
                    }
                    if (!isInserted.get(node).contains(v)) {
                        isInserted.get(node).add(v);
                        phis.get(node).add(new PhiStmt(v, v, new PhiExp()));
                        current.add(node);
                    }
                }
            }
        }

        for (int i = 0; i < graph.size(); ++i) {
            if (phis.get(i) != null) {
                graph.getNode(i).insertStmts(phis.get(i));
            }
        }
    }

    private int stmtCount = 0;

    private int safeEstimationForPhiVars;

    private IndexMap<Var, PhiStmt> correspondingPhiStmts;

    private SparseSet isPhiDefiningVar; // only for debugging

    private SparseSet isUseless;

    private SparseSet stack;

    private void prepareForPruning() {
        /*
         * To leverage Var.index in searching, we have to estimate the maximum count of
         * all the variables, including the potential added variables for SSA process.
         * the formula is as follows:
         * currentVarCount: the count of variables before renaming
         * blockCount: later to be multiplied by oldCount to represent the safe estimation of the
         *             possible phi insertions for every old variable in every block.
         * stmtCount: the safe estimation of the renaming.
         * the maximum count of all the variables = oldCount + oldCount * blockCount + stmtCount
         */
        int currentVarCount = vars.size();
        safeEstimationForPhiVars =
                currentVarCount + currentVarCount * graph.size() + stmtCount;

        correspondingPhiStmts = new IndexMap<>(indexer, safeEstimationForPhiVars);
        isPhiDefiningVar = DEBUG ?
                new SparseSet(safeEstimationForPhiVars, safeEstimationForPhiVars) : null;
        isUseless = new SparseSet(safeEstimationForPhiVars, safeEstimationForPhiVars);
        stack = new SparseSet(safeEstimationForPhiVars, safeEstimationForPhiVars);
    }

    /**
     * source: <a href="https://pfalcon.github.io/ssabook/latest/book-full.pdf">SSA Book</a>
     * Algorithm 3.3
     */
    private void renaming() {
        List<Var> params = manager.getParams(); // `params` does not contain `this`.
        IndexMap<Var, Integer> incId = new IndexMap<>(indexer, nonSSAVars.length);
        for (Var v : nonSSAVars) {
            if (params.contains(v)) {
                incId.put(v, 2); // params are born with def.
            } else {
                incId.put(v, 1);
            }
        }
        // Initial definitions of parameters.
        // And the rest vars could be left to null safely because JVM based languages are strict.
        IndexMap<Var, Var> entryDefs = new IndexMap<>(indexer, nonSSAVars.length);
        for (Var p : params) {
            entryDefs.put(p, p);
        }
        IndexMap<Var, Var>[] reachingDefsForBlocks = new IndexMap[graph.size()];
        // Set defs for actual entry node.
        reachingDefsForBlocks[postOrder[graph.size() - 1]] = entryDefs;
        // Propagate params defs to the phis leading the actual entry node.
        propagatePhisToSucc(null, entryDefs, graph.getEntry()); // null to represent params.

        /* The traversal order in the original algorithm is stricter than needed. Indeed, Here it
         * just has to satisfy that when traversed, the current reachingDef is the def that
         * dominates the current program point.
         * Reverse post order is a depth-right first traversal which satisfies the data dependency.
         * Use it for efficiency, the cost is the number of variables are not in order.
         */
        for (int i = graph.size() - 1; i >= 0; --i) {
            int node = postOrder[i];
            IndexMap<Var, Var> reachingDefs = new IndexMap<>(reachingDefsForBlocks[dom[node]]);
            Block block = graph.getNode(node);
            List<Stmt> newStmts = new ArrayList<>(block.getStmts().size());
            TriFunction<Stmt, Var, Map<Var, Var>, Pair<Stmt, Var>> replaceStmtAndUpdateDefs =
                    (stmt, base, uses) -> {
                Var freshVar = getFreshDefs(incId, base);
                Lenses lenses = new Lenses(method, uses, Map.of(base, freshVar));
                Stmt newStmt = lenses.subSt(stmt);
                newStmts.add(newStmt);
                reachingDefs.put(base, freshVar);
                return new Pair<>(newStmt, freshVar);
            };
            for (Stmt stmt : block.getStmts()) {
                /*
                 * The possible enumerations of the conditions are as follows:
                 *   | PhiStmt | hasDefToBeSSA | Catch(which defines the catch var) | Other |
                 *   |    T    |       T       |                 F                  |   F   |
                 *   |    F    |       T       |                 F                  |   F   |
                 *   |    F    |       F       |                 T                  |   F   |
                 *   |    F    |       F       |                 F                  |   T   |
                 */
                if (stmt instanceof PhiStmt phiStmt) {
                    // only update uses for non-phi stmts.
                    Pair<Stmt, Var> p =
                            replaceStmtAndUpdateDefs.apply(phiStmt, phiStmt.getBase(), Map.of());
                    PhiStmt newStmt = (PhiStmt) p.first();
                    Var freshVar = p.second();
                    // place initial marking phase for pruning step for performance
                    if (DEBUG) {
                        isPhiDefiningVar.add(freshVar.getIndex());
                    }
                    isUseless.add(freshVar.getIndex()); // Collect for pruning.
                    correspondingPhiStmts.put(freshVar, newStmt);
                } else if (
                        stmt instanceof DefinitionStmt<?, ?> defStmt
                                && defStmt.getLValue() instanceof Var base
                                && isVarToBeSSA(base)
                ) {
                    Pair<Stmt, Var> p =
                            replaceStmtAndUpdateDefs.apply(defStmt, base, reachingDefs);
                    Stmt newStmt = p.first();
                    markUsefulForStmt(newStmt);
                } else if (stmt instanceof Catch catchStmt) {
                    Pair<Stmt, Var> p = replaceStmtAndUpdateDefs.apply(
                            catchStmt,
                            catchStmt.getExceptionRef(),
                            reachingDefs
                    );
                    Stmt newStmt = p.first();
                    markUsefulForStmt(newStmt);
                } else {
                    // just replace the stmt by updating the uses
                    Lenses lenses = new Lenses(method, reachingDefs, Map.of());
                    Stmt newStmt = lenses.subSt(stmt);
                    newStmts.add(newStmt);
                    markUsefulForStmt(newStmt);
                }
            }
            block.setStmts(newStmts);

            propagateDefsToSuccBlocksPhis(block, reachingDefs);

            reachingDefsForBlocks[node] = reachingDefs;
        }
    }

    private Var getFreshDefs(IndexMap<Var, Integer> incId, Var base) {
        int id = incId.get(base);
        incId.put(base, 1 + id);
        return manager.splitVar(base, id);
    }

    private void markUsefulForStmt(Stmt stmt) {
        /* Collect use for pruning step.
         * In this situation, "An use x is defined by some phi function" equals to
         * "isUseless.has(x) || stack.has(x)" because of the dominance order here.
         * (Before pruning, stack.has(x) implies that x is phi defining var and useful.)
         */
        if (DEBUG) {
            Predicate<RValue> p = r -> {
                if (!(r instanceof Var x)) return true;
                boolean visited =
                        isUseless.has(x.getIndex()) || stack.has(x.getIndex());
                return isPhiDefiningVar.has(x.getIndex()) == visited;
            };
            assert stmt.getUses()
                    .stream()
                    .allMatch(p);
        }
        StmtVarVisitor.visitUse(stmt, this::markUseful);
    }

    private void propagatePhisToSucc(Block pred, IndexMap<Var, Var> reachingDefs, Block succ) {
        int succNode = graph.getIndex(succ);
        if (phis.get(succNode) == null) {
            return;
        }
        for (Stmt p : phis.get(succNode)) {
            PhiStmt phi = (PhiStmt) p;
            Var base = phi.getBase();
            Var reachingDef = reachingDefs.get(base);
            if (reachingDef != null) {
                // if such reaching def doesn't exist (is null), due to the strictness
                // of Java (maybe all the JVM based languages), the phi stmt is dead,
                // and it would be pruned in the pruning step.
                // for example,
                //                 // no reaching def for c here
                // -> c1 = \phi(c) // c is defined in the loop, not in this scope,
                //                    but the phi stmt is still inserted by the algorithm
                //    while (...) {
                //       c = 1
                //       ...
                //    }
                phi.getRValue().addUseAndCorrespondingBlocks(reachingDef, pred);
            }
        }
    }

    private void propagateDefsToSuccBlocksPhis(Block block, IndexMap<Var, Var> reachingDefs) {
        for (Block succ : graph.outEdges(block)) {
            propagatePhisToSucc(block, reachingDefs, succ);
        }
    }

    private boolean isVarToBeSSA(Var v) {
        return !v.isConst()
                // copy from VarManager.isNotSpecialVar()
                && !v.getName().startsWith("*") && !Objects.equals(v.getName(), "$null")
                && v != manager.getThisVar()
                ;
    }

    private void markUseful(RValue rValue) {
        /* Those rValues who are not Var are ignored without recursive traversal through
         * its inner uses. It is because the call to Stmt.getUses() have already returned
         * the inner uses of the rValues, so there is no need to process again.
         */
        if (rValue instanceof Var var) {
            if (isUseless.has(var.getIndex())) {
                isUseless.delete(var.getIndex());
                stack.add(var.getIndex());
            }
        }
    }

    /**
     * source: <a href="https://pfalcon.github.io/ssabook/latest/book-full.pdf">SSA Book</a>
     * Algorithm 3.7
     */
    private void pruning() {
        // initial marking phase has been done in renaming step
        if (DEBUG) {
            for (int i : isPhiDefiningVar) {
                assert isUseless.has(i) || stack.has(i);
            }
        }

        // usefulness propagation phase
        while (!stack.isEmpty()) {
            Var a = vars.get(stack.removeLast());
            PhiStmt phiStmt = correspondingPhiStmts.get(a);
            phiStmt.getRValue().getUsesAndInBlocks().forEach(p -> markUseful(p.first()));
        }

        // final pruning phase
        for (int i = 0; i < graph.size(); i++) {
            IBasicBlock node = graph.getNode(i);
            node.getStmts().removeIf(
                    s -> s instanceof PhiStmt p && isUseless.has(p.getLValue().getIndex()));
        }
    }

    private void resumeNames() {
        VarManager manager = (VarManager) this.manager;
        if (!manager.existsLocalVariableTable) return;

        for (int n = 0; n < graph.size(); n++) {
            BytecodeBlock block = (BytecodeBlock) graph.getNode(n);
            int phiBias = 0;
            for (int i = 0; i < block.getStmts().size(); i++) {
                Stmt stmt = block.getStmts().get(i);
                if (stmt instanceof DefinitionStmt<?,?> definitionStmt
                        && definitionStmt.getLValue() instanceof Var def) {
                    int queryIndex;
                    if (definitionStmt instanceof PhiStmt) {
                        phiBias++;
                        queryIndex = 0;
                    } else {
                        queryIndex = i - phiBias;
                    }
                    if (def.getName().startsWith(VarManager.LOCAL_PREFIX)) {
                        Optional<String> maybeName =
                                manager.getName(manager.getSlot(def), block.getOrig(queryIndex));
                        maybeName.ifPresent(s -> manager.fixName(def, s));
                    }
                } else if (stmt instanceof Catch c) {
                    // for Catch stmt, i == 0
                    assert i == 0;
                    Var local = c.getExceptionRef();
                    Optional<String> maybeName =
                            manager.getName(manager.getSlot(local), block.getOrig(i));
                    maybeName.ifPresent(s -> manager.fixName(local, s));
                }
            }
        }
    }

    private void removePrunedVars() {
        List<Integer> removed = isUseless.toList();
        manager.removeAndReindexVars(var -> removed.contains(var.getIndex()));
    }
}
