package pascal.taie.frontend.newfrontend;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.ArraySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Def-use information
 */
public class DUInfo {
    List<Set<IBasicBlock>> defBlocks; // For performance, you may change the implementation for set

    public DUInfo(int varCount) {
        defBlocks = new ArrayList<>(varCount);
        for (int i = 0; i < varCount; i++) {
            defBlocks.add(new ArraySet<>());
        }
    }

    public void addDefBlock(Var v, IBasicBlock b) {
        int i = v.getIndex();
        while (i >= defBlocks.size()) {
            defBlocks.add(new ArraySet<>());
        }
        defBlocks.get(i).add(b);
    }

    public Set<IBasicBlock> getDefBlock(Var v) {
        int i = v.getIndex();
        while (i >= defBlocks.size()) {
            // Maybe we can directly return an empty list?
            defBlocks.add(new ArraySet<>());
        }
        return defBlocks.get(i);
    }
}
