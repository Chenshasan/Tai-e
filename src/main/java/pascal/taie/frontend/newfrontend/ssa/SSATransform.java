package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.DUInfo;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.IVarManager;
import pascal.taie.frontend.newfrontend.Lenses;
import pascal.taie.frontend.newfrontend.SparseSet;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexMap;
import pascal.taie.util.collection.IndexerBitSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public SSATransform(JMethod method, IndexedGraph<Block> graph, IVarManager manager, DUInfo info) {
        this.method = method;
        this.graph = graph;
        Dominator<Block> dominator = new Dominator<>(graph);
        this.df = dominator.getDF();
        this.dom = dominator.getDomTree();
        this.postOrder = dominator.getPostOrder();
        this.manager = manager;
        this.vars = manager.getVars();
        this.info = info;
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
        // testify whether step 1.5 creates those data structures that is big enough
        assert safeEstimationForPhiVars >= vars.size();
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
            if (inEdges(i).size() >= 2) {
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
            List<IBasicBlock> defBlocks = info.getDefBlock(v);
            for (IBasicBlock block : defBlocks) {
                current.add(block.getIndex());
            }
            while (!current.isEmpty()) {
                IBasicBlock block = graph.getNode(current.removeLast());
                for (int node : df.get(block.getIndex())) {
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
        isPhiDefiningVar = new SparseSet(safeEstimationForPhiVars, safeEstimationForPhiVars);
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
        // Initial definitions of parameters for the pseudo entry.
        // And the rest vars could be left to null safely because JVM based languages are strict.
        IndexMap<Var, Var> entryDefs = new IndexMap<>(indexer, nonSSAVars.length);
        for (Var p : params) {
            entryDefs.put(p, p);
        }
        IndexMap<Var, Var>[] reachingDefsForBlocks = new IndexMap[graph.size()];
        reachingDefsForBlocks[postOrder[graph.size() - 1]] = entryDefs;

        // Reverse post order is a depth-right first traversal which satisfies the data dependency.
        // Use it for efficiency, the cost is the number of variables are not in order.
        for (int i = graph.size() - 2; i >= 0; --i) { // i = graph.size()-2 or -1?
            int node = postOrder[i];
            IndexMap<Var, Var> reachingDefs = new IndexMap<>(reachingDefsForBlocks[dom[node]]);
            Block block = graph.getNode(node);
            List<Stmt> newStmts = new ArrayList<>(block.getStmts().size());
            for (Stmt stmt : block.getStmts()) {
                // only update uses for non-phi stmts.
                Map<Var, Var> uses = stmt instanceof PhiStmt ? Map.of() : reachingDefs;
                Map<Var, Var> def;
                Var potentialBase = null;
                Var freshVar = null;
                if (stmt instanceof DefinitionStmt<?, ?> defStmt
                        && defStmt.getLValue() instanceof Var base
                        && isVarToBeSSA(base)
                ) {
                    // In the first (and only) time that a phi stmt is visited, its def is its
                    // base according to the callsite of the init method in `phiInsertion`.
                    assert !(stmt instanceof PhiStmt p) || p.getBase() == p.getLValue();
                    potentialBase = base;
                    int id = incId.get(base);
                    incId.put(base, 1 + id);
                    if (id == 1) {
                        freshVar = base;
                    } else {
                        freshVar = manager.splitVar(base, id);
                        freshVar.setType(base.getType());
                    }
                    def = Map.of(base, freshVar);
                } else {
                    def = Map.of();
                }
                Lenses lenses = new Lenses(method, uses, def);
                Stmt newStmt = lenses.subSt(stmt);
                newStmts.add(newStmt);
                if (freshVar != null) {
                    reachingDefs.put(potentialBase, freshVar);
                }
                if (newStmt instanceof PhiStmt phiStmt) {
                    isPhiDefiningVar.add(freshVar.getIndex());
                    isUseless.add(freshVar.getIndex()); // Collect for pruning.
                    correspondingPhiStmts.put(freshVar, phiStmt);
                }
                else {
                    /* Collect use for pruning step.
                     * In this situation, "An use x is defined by some phi function" equals to
                     * "isUseless.has(x) || stack.has(x)" because of the dominance order here.
                     * (Before pruning, stack.has(x) implies that x is phi defining var and useful.)
                     */
                    assert newStmt.getUses()
                            .stream()
                            .allMatch(r -> {
                                if (!(r instanceof Var x)) return true;
                                boolean cond =
                                        isUseless.has(x.getIndex()) || stack.has(x.getIndex());
                                return isPhiDefiningVar.has(x.getIndex()) == cond;
                            });
                    newStmt.getUses().forEach(this::markUseful);
                }
            }
            block.setStmts(newStmts);

            for (Block succ : graph.outEdges(block)) {
                int succNode = graph.getIndex(succ);
                if (phis.get(succNode) == null) {
                    continue;
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
                        phi.getRValue().addUseAndCorrespondingBlocks(reachingDef, block);
                    }
                }
            }

            reachingDefsForBlocks[node] = reachingDefs;
        }
    }

    private boolean isVarToBeSSA(Var v) {
        return v.getIndex() < nonSSAVars.length;
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
    public void pruning() {
        // initial marking phase has been done in renaming step
        for (int i : isPhiDefiningVar) {
            assert isUseless.has(i) || stack.has(i);
        }

        // usefulness propagation phase
        while (!stack.isEmpty()) {
            Var a = vars.get(stack.removeLast());
            PhiStmt phiStmt = correspondingPhiStmts.get(a);
            phiStmt.getUses().forEach(this::markUseful);
        }

        // final pruning phase
        for (int i = 0; i < graph.size(); i++) {
            IBasicBlock node = graph.getNode(i);
            node.getStmts().removeIf(
                    s -> s instanceof PhiStmt p && isUseless.has(p.getLValue().getIndex()));
        }
    }
}
