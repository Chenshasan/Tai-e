package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public final class BytecodeBlock {
    private final LabelNode start;
    private final List<AbstractInsnNode> instr;
    private final List<BytecodeBlock> inEdges;
    private final List<BytecodeBlock> outEdges;

    @Nullable
    private BytecodeBlock fallThrough;

    private boolean complete;

    public BytecodeBlock(
            LabelNode start,
            List<AbstractInsnNode> instr,
            List<BytecodeBlock> inEdges,
            List<BytecodeBlock> outEdges,
            @Nullable
            BytecodeBlock fallThrough) {
        this.start = start;
        this.instr = instr;
        this.inEdges = inEdges;
        this.outEdges = outEdges;
        this.fallThrough = fallThrough;
        this.complete = false;
    }

    public LabelNode start() {
        return start;
    }

    public List<AbstractInsnNode> instr() {
        return instr;
    }

    public List<BytecodeBlock> inEdges() {
        return inEdges;
    }

    public List<BytecodeBlock> outEdges() {
        return outEdges;
    }

    @Nullable
    public BytecodeBlock fallThrough() {
        return fallThrough;
    }

    public void setFallThrough(BytecodeBlock fallThrough) {
        this.fallThrough = fallThrough;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete() {
        complete = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BytecodeBlock) obj;
        return Objects.equals(this.start, that.start) &&
                Objects.equals(this.instr, that.instr) &&
                Objects.equals(this.inEdges, that.inEdges) &&
                Objects.equals(this.outEdges, that.outEdges) &&
                Objects.equals(this.fallThrough, that.fallThrough);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, instr, inEdges, outEdges, fallThrough);
    }

    @Override
    public String toString() {
        return "BytecodeBlock[" +
                "start=" + start + ", " +
                "instr=" + instr + ", " +
                "inEdges=" + inEdges + ", " +
                "outEdges=" + outEdges + ", " +
                "fallThrough=" + fallThrough + ']';
    }

}
