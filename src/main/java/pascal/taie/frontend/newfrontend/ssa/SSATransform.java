package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.DUInfo;
import pascal.taie.frontend.newfrontend.IBasicBlock;
import pascal.taie.frontend.newfrontend.IVarManager;
import pascal.taie.frontend.newfrontend.SparseSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.IndexerBitSet;

import java.util.ArrayList;
import java.util.List;

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
    private final IndexedGraph<Block> graph;

    private final Dominator.DominatorFrontiers df;

    private final IVarManager manager;

    private final DUInfo info;

    public SSATransform(IndexedGraph<Block> graph, IVarManager manager, DUInfo info) {
        this.graph = graph;
        this.df = new Dominator<>(graph).getDF();
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

    private void phiInsertion() {
        // index with block.getIndex()
        List<IndexerBitSet<Var>> isInserted = new ArrayList<>();
        List<List<Stmt>> phis = new ArrayList<>();

        Var[] vars = manager.getNonSSAVar();
        Indexer<Var> indexer = new Indexer<>() {
            @Override
            public int getIndex(Var o) {
                return o.getIndex();
            }

            @Override
            public Var getObject(int index) {
                throw new UnsupportedOperationException();
            }
        };
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
        for (Var v : vars) {
            List<IBasicBlock> defBlocks = info.getDefBlock(v);
            for (IBasicBlock block : defBlocks) {
                current.add(block.getIndex());
            }
            while (!current.isEmpty()) {
                IBasicBlock block = graph.getNode(current.removeLast());
                for (int node : df.get(block.getIndex())) {
                    if (!isInserted.get(node).contains(v)) {
                        isInserted.get(node).add(v);
                        phis.get(node).add(new PhiStmt(v));
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

    private void renaming() {
    }

    public void pruning() {
    }
}
