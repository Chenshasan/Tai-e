package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

public record CryptoObjPropagate(JMethod method, int from, int to, Type type) {

    /**
     * Special number representing the base variable.
     */
    static final int BASE = -1;

    /**
     * String representation of base variable.
     */
    private static final String BASE_STR = "base";

    /**
     * Special number representing the variable that receivers
     * the result of the invocation.
     */
    static final int RESULT = -2;

    /**
     * String representation of result variable
     */
    private static final String RESULT_STR = "result";

    @Override
    public String toString() {
        return method + ": " + toString(from) + " -> " + toString(to) +
                "(" + type + ")";
    }

    /**
     * Coverts string to index.
     */
    static int toInt(String s) {
        return switch (s.toLowerCase()) {
            case BASE_STR -> BASE;
            case RESULT_STR -> RESULT;
            default -> Integer.parseInt(s);
        };
    }

    /**
     * Converts index to string.
     */
    private static String toString(int index) {
        return switch (index) {
            case BASE -> BASE_STR;
            case RESULT -> RESULT_STR;
            default -> Integer.toString(index);
        };
    }
}
