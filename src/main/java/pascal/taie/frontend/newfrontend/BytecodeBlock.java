package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class BytecodeBlock implements IBasicBlock {
    private final LabelNode label;

    private int index = -1;

    private AsmListSlice instr;

    private Stack<StackItem> inStack;

    private Stack<StackItem> outStack;

    private List<Stmt> stmts;

    private FrameNode frame;

    private boolean complete;

    @Nullable
    private final Type exceptionHandlerType;

    private List<Object> frameLocalType;

    private int[] stmt2Asm;

    private boolean isInTry = false;

    public BytecodeBlock(LabelNode label, @Nullable BytecodeBlock fallThrough) {
        this(label, fallThrough, null);
    }

    public BytecodeBlock(LabelNode label, @Nullable BytecodeBlock fallThrough, @Nullable Type exceptionHandlerType) {
        this.label = label;
        this.stmts = new ArrayList<>();
        this.complete = false;
        this.exceptionHandlerType = exceptionHandlerType;
    }

    public LabelNode label() {
        return label;
    }

    public AsmListSlice instr() {
        return instr;
    }

    public void setInstr(AsmListSlice instr) {
        this.instr = instr;
    }

    public boolean isCatch() {
        return getExceptionHandlerType() != null;
    }

    public void setIsInTry() {
        isInTry = true;
    }

    public boolean isInTry() {
        return isInTry;
    }

    public void setComplete() {
        complete = true;
    }

    public Stack<StackItem> getInStack() {
        return inStack;
    }

    public Stack<StackItem> getOutStack() {
        return outStack;
    }

    public void setInStack(Stack<StackItem> inStack) {
        assert this.inStack == null : "InStack should not be assigned multiple times.";
//        assert frame == null ||
//                inStack.stream().filter(i -> i instanceof Var).count() == frame.stack.size();
        this.inStack = inStack;
//        AbstractInsnNode node = instr.get(0);
//        int popHeight = 0;
//        while (node != null &&
//                (node.getOpcode() == Opcodes.POP || node.getOpcode() == Opcodes.POP2)) {
//            if (node.getOpcode() == Opcodes.POP) {
//                popHeight++;
//            } else {
//                popHeight += 2;
//            }
//            node = node.getNext();
//        }
//        if (popHeight == inStack.size()) {
//            return;
//        }
//        for (var pred : inEdges) {
//            if (pred.outStack == null) {
//                pred.setOutStack(inStack);
//            }
//        }
    }

    public void setOutStack(Stack<StackItem> outStack) {
        assert this.outStack == null : "OutStack should not be assigned multiple times.";
        this.outStack = outStack;
//        for (var succ : outEdges) {
//            if (succ.inStack == null) {
//                succ.setInStack(outStack);
//            }
//        }
    }

    public AbstractInsnNode getLastBytecode() {
        return instr.get(instr.size() - 1);
    }

    public Stmt getLastStmt() {
        assert !stmts.isEmpty();
        return stmts.get(stmts.size() - 1);
    }

    public List<Stmt> getStmts() {
        return stmts;
    }

    @Override
    public void setStmt(Stmt stmt, int pos) {
        stmts.set(pos, stmt);
    }

    @Override
    public void insertStmts(List<Stmt> stmts) {
        // if this block is a catch block,
        // then we should insert the stmts after the first catch stmt
        List<Stmt> temp = new ArrayList<>(stmts.size() + this.stmts.size());
        int i = 0;
        if (isCatch()) {
            // if this block is a catch block, then the first stmt must be a catch stmt
            assert this.stmts.get(0) instanceof Catch;
            temp.add(this.stmts.get(0));
            temp.addAll(stmts);
            i++;
        } else {
            temp.addAll(stmts);
        }
        for (; i < this.stmts.size(); ++i) {
            temp.add(this.stmts.get(i));
        }
        this.stmts = temp;
    }

    @Override
    public void setStmts(List<Stmt> stmts) {
        this.stmts = stmts;
    }

    @Nullable
    public FrameNode getFrame() {
        return frame;
    }

    public Map<Var, Type> getInitTyping() {
        assert frame != null;

        Map<Var, Type> typing = Maps.newMap();

        if (inStack != null) {
            int n = 0;
            for (int i = 0; i < frame.stack.size(); ++i) {
                Exp e = inStack.get(n).e();
                Var v;
                if (e instanceof Top) {
                    n++;
                    e = inStack.get(n).e();
                }

                if (e instanceof Phi phi) {
                    v = phi.getVar();
                } else if (e instanceof Var v1) {
                    v = v1;
                } else {
                    v = null;
                }
                if (v != null) {
                    typing.put(v, Utils.fromAsmFrameType(frame.stack.get(i)));
                }
                n++;
            }
        }
        return typing;
    }

    private void buildFrameLocalType() {
        frameLocalType = new ArrayList<>(frame.local.size() + 1);
        int n = 0;
        for (Object o : frame.local) {
            frameLocalType.add(o);
            // is long or double
            if (o instanceof Integer i && (i == 3 || i == 4)) {
                frameLocalType.add(0); // place top
                n += 2;
            } else {
                n += 1;
            }
        }
        tryCorrectFrame(n);
        if (inStack != null) {
            n = 0;
            for (StackItem item : inStack) {
                Exp e = item.e();
                if (e == Top.Top) {
                    continue;
                }
                if (e instanceof Var v) {
                    int slot = VarManager.getSlotFast(v);
                    if (slot != -1) {
                        for (int k = frameLocalType.size(); k <= slot; ++k) {
                            frameLocalType.add(0);
                        }
                        frameLocalType.set(slot, frame.stack.get(n));
                    }
                }
                n++;
            }
        }
    }

    private void ensureLocalType() {
        if (frameLocalType == null) {
            buildFrameLocalType();
        }
    }

    public boolean isLocalExistInFrame(int slot) {
        ensureLocalType();
        if (slot >= frameLocalType.size()) {
            return false;
        }
        Object o = frameLocalType.get(slot);
        return ! (o instanceof Integer i && i == 0);
    }

    public List<Object> getFrameLocalType() {
        ensureLocalType();
        return frameLocalType;
    }

    public void setFrame(FrameNode frame) {
        assert frame != null;
        this.frame = frame;
    }

    void setStmt2Asm(int[] stmt2Asm) {
        this.stmt2Asm = stmt2Asm;
    }

    public AbstractInsnNode getOrig(int index) {
        return instr.get(stmt2Asm[index]);
    }

    public void setIndex(int i) {
        this.index = i;
    }

    public int getIndex() {
        return this.index;
    }

    @Nullable
    public Type getExceptionHandlerType() {
        return exceptionHandlerType;
    }


    private void tryCorrectFrame(int size) {
        if (instr.isEmpty()) {
            return;
        }
        AbstractInsnNode last = instr.get(instr.size() - 1);
        if (!(Utils.isReturn(last) || Utils.isThrow(last)) || isInTry) {
            return;
        }
        // the last node is return

        boolean[] hits = new boolean[size];
        Arrays.fill(hits, false);
        boolean[] redefines = new boolean[size];
        Arrays.fill(redefines, false);

        for (AbstractInsnNode insnNode : instr) {
            if (insnNode instanceof VarInsnNode varInsnNode) {
                int var = varInsnNode.var;
                if (var >= size) {
                    continue;
                }
                if (Utils.isVarStore(varInsnNode)) {
                    redefines[var] = true;
                } else {
                    if (!redefines[var]) {
                        hits[var] = true;
                    }
                }
            } else if (insnNode instanceof IincInsnNode iincInsnNode) {
                int var = iincInsnNode.var;
                if (var >= size) {
                    continue;
                }
                redefines[var] = true;
                hits[var] = true;
            }
        }

        for (int i = 0; i < frameLocalType.size(); ++i) {
            if (!hits[i]) {
                frameLocalType.set(i, 0);
            }
        }
    }
}
