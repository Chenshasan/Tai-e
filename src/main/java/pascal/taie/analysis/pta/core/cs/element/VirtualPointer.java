package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.language.type.Type;

/**
 * a virtual pointer variable
 * <br>
 * TODO: refactor in tai-e official version
 */
public class VirtualPointer extends AbstractPointer {

    private final Object key;

    private final Type type;

    public VirtualPointer(Object key,
                          Type type) {
        super(0);
        this.key = key;
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirtualPointer{" +
            "key=" + key +
            ", type=" + type +
            '}';
    }

    @Override
    public Type getType() {
        return type;
    }
}
