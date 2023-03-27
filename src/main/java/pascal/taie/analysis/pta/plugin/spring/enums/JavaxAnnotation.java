package pascal.taie.analysis.pta.plugin.spring.enums;

import javax.annotation.Nullable;

public enum JavaxAnnotation implements AnnotationEnum {

    /* from javax.annotation */
    RESOURCE("javax.annotation.Resource"),

    ;

    public final String name;

    JavaxAnnotation(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    public static JavaxAnnotation of(String name) {
        for (JavaxAnnotation value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
