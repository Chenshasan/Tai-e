package pascal.taie.analysis.pta.plugin.spring.enums;

public enum SpringConfiguration {
    APPLICATION_NAME("spring.application.name"),

    ;

    public final String key;

    SpringConfiguration(String key) {
        this.key = key;
    }
}
