package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.DUInfo;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.IVarManager;
import pascal.taie.frontend.newfrontend.Lenses;
import pascal.taie.frontend.newfrontend.SparseSet;
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

    private final DUInfo info;

    public SSATransform(JMethod method, IndexedGraph<Block> graph, IVarManager manager, DUInfo info) {
        this.method = method;
        this.graph = graph;
        Dominator<Block> dominator = new Dominator<>(graph);
        this.df = dominator.getDF();
        this.dom = dominator.getDomTree();
        this.postOrder = dominator.getPostOrder();
        this.manager = manager;
        this.info = info;
    }

    public void build() {
        // step 1. insert phi functions
        phiInsertion();
        // step 2. rename variables
        renaming();
        // step 3. remove dead phi functions
        pruning();
    }

    private List<Block> inEdges(int i) {
        return graph.inEdges(graph.getNode(i));
    }

    private final List<IndexerBitSet<Var>> isInserted = new ArrayList<>();
    private final List<List<Stmt>> phis = new ArrayList<>();
    private Var[] nonSSAVars;

    Indexer<Var> indexer = new Indexer<>() {
        @Override
        public int getIndex(Var o) {
            if (o == null) return -1;
            return o.getIndex();
        }

        @Override
        public Var getObject(int index) {
            return nonSSAVars[index];
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

    /**
     * source: <a href="https://pfalcon.github.io/ssabook/latest/book-full.pdf">SSA Book</a>
     * Algorithm 3.3
     */
    private void renaming() {
        IndexMap<Var, Integer> incId = new IndexMap<>(indexer, nonSSAVars.length);
        for (Var v : nonSSAVars) {
            incId.put(v, 0);
        }
        IndexMap<Var, Var>[] reachingDefsForBlocks = new IndexMap[graph.size()];
        // Empty initialization for the pseudo entry because JVM based languages are strict.
        reachingDefsForBlocks[postOrder[graph.size() - 1]] =
                new IndexMap<>(indexer, nonSSAVars.length);

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
                Var newVar = null;
                if (stmt instanceof DefinitionStmt<?, ?> defStmt
                        && defStmt.getLValue() instanceof Var base
                        && isNonSSAVar(base)
                ) {
                    // In the first (and only) time that a phi stmt is visited, its def is its
                    // base according to the callsite of the init method in `phiInsertion`.
                    assert !(stmt instanceof PhiStmt p) || p.getBase() == p.getLValue();
                    potentialBase = base;
                    int id = incId.get(base);
                    incId.put(base, 1 + id);
                    if (id == 0) {
                        newVar = base;
                    } else {
                        newVar = manager.splitVar(base, id);
                    }
                    def = Map.of(base, newVar);
                } else {
                    def = Map.of();
                }
                Lenses lenses = new Lenses(method, uses, def);
                Stmt newStmt = lenses.subSt(stmt);
                newStmts.add(newStmt);
                if (potentialBase != null) {
                    reachingDefs.put(potentialBase, newVar);
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
                    if (reachingDef == null) {
                        // if such reaching def doesn't exist, the phi stmt is dead
                        // for example,
                        //                 // no reaching def for c here
                        // -> c1 = \phi(c) // c is defined in the loop, not in this scope,
                        //                    but the phi stmt is still inserted by the algorithm
                        //    while (...) {
                        //       c = 1
                        //       ...
                        //    }
                        phi.markDead();
                    } else {
                        phi.getRValue().addUseAndCorrespondingBlocks(reachingDef, block);
                    }
                }
            }

            reachingDefsForBlocks[node] = reachingDefs;
        }
    }

    private boolean isNonSSAVar(Var v) {
        return v.getIndex() < nonSSAVars.length;
    }

    public void pruning() {
    }
}
