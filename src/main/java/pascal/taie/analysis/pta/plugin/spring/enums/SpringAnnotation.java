package pascal.taie.analysis.pta.plugin.spring.enums;

import javax.annotation.Nullable;

public enum SpringAnnotation implements AnnotationEnum {

    /* from spring-context */
    BEAN("org.springframework.context.annotation.Bean"),
    COMPONENT_SCAN("org.springframework.context.annotation.ComponentScan"),
    CONFIGURATION("org.springframework.context.annotation.Configuration"),
    COMPONENT_SCANS("org.springframework.context.annotation.ComponentScans"),
    CONDITIONAL("org.springframework.context.annotation.Conditional"),
    DEPENDS_ON("org.springframework.context.annotation.DependsOn"),
    DESCRIPTION("org.springframework.context.annotation.Description"),
    SCOPE("org.springframework.context.annotation.Scope"),
    PROPERTY_SOURCE("org.springframework.context.annotation.PropertySource"),
    PROPERTY_SOURCES("org.springframework.context.annotation.PropertySources"),
    ROLE("org.springframework.context.annotation.Role"),
    COMPONENT("org.springframework.stereotype.Component"),
    CONTROLLER("org.springframework.stereotype.Controller"),
    INDEXED("org.springframework.stereotype.Indexed"),
    REPOSITORY("org.springframework.stereotype.Repository"),
    SERVICE("org.springframework.stereotype.Service"),

    /* from spring-beans */
    AUTOWIRED("org.springframework.beans.factory.annotation.Autowired"),
    CONFIGURABLE("org.springframework.beans.factory.annotation.Configurable"),
    LOOKUP("org.springframework.beans.factory.annotation.Lookup"),
    QUALIFIER("org.springframework.beans.factory.annotation.Qualifier"),
    VALUE("org.springframework.beans.factory.annotation.Value"),

    /* from spring-web */
    CONTROLLER_ADVICE("org.springframework.web.bind.annotation.ControllerAdvice"),
    COOKIE_VALUE("org.springframework.web.bind.annotation.CookieValue"),
    CROSS_ORIGIN("org.springframework.web.bind.annotation.CrossOrigin"),
    DELETE_MAPPING("org.springframework.web.bind.annotation.DeleteMapping"),
    EXCEPTION_HANDLER("org.springframework.web.bind.annotation.ExceptionHandler"),
    GET_MAPPING("org.springframework.web.bind.annotation.GetMapping"),
    INIT_BINDER("org.springframework.web.bind.annotation.InitBinder"),
    MAPPING("org.springframework.web.bind.annotation.Mapping"),
    MATRIX_VARIABLE("org.springframework.web.bind.annotation.MatrixVariable"),
    MODEL_ATTRIBUTE("org.springframework.web.bind.annotation.ModelAttribute"),
    PATCH_MAPPING("org.springframework.web.bind.annotation.PatchMapping"),
    PATH_VARIABLE("org.springframework.web.bind.annotation.PathVariable"),
    POST_MAPPING("org.springframework.web.bind.annotation.PostMapping"),
    PUT_MAPPING("org.springframework.web.bind.annotation.PutMapping"),
    REQUEST_ATTRIBUTE("org.springframework.web.bind.annotation.RequestAttribute"),
    REQUEST_BODY("org.springframework.web.bind.annotation.RequestBody"),
    REQUEST_HEADER("org.springframework.web.bind.annotation.RequestHeader"),
    REQUEST_MAPPING("org.springframework.web.bind.annotation.RequestMapping"),
    REQUEST_METHOD("org.springframework.web.bind.annotation.RequestMethod"),
    REQUEST_PARAM("org.springframework.web.bind.annotation.RequestParam"),
    REQUEST_PART("org.springframework.web.bind.annotation.RequestPart"),
    RESPONSE_BODY("org.springframework.web.bind.annotation.ResponseBody"),
    RESPONSE_STATUS("org.springframework.web.bind.annotation.ResponseStatus"),
    REST_CONTROLLER("org.springframework.web.bind.annotation.RestController"),
    REST_CONTROLLER_ADVICE("org.springframework.web.bind.annotation.RestControllerAdvice"),
    SESSION_ATTRIBUTE("org.springframework.web.bind.annotation.SessionAttribute"),
    SESSION_ATTRIBUTES("org.springframework.web.bind.annotation.SessionAttributes"),
    VALUE_CONSTANTS("org.springframework.web.bind.annotation.ValueConstants"),

    /* from spring-boot */
    CONFIGURATION_PROPERTIES("org.springframework.boot.context.properties.ConfigurationProperties"),
    ENABLE_AUTO_CONFIGURATION("org.springframework.boot.autoconfigure.EnableAutoConfiguration"),

    /* from spring-boot-auto-configuration */
    SPRING_BOOT_APPLICATION("org.springframework.boot.autoconfigure.SpringBootApplication"),
    SPRING_BOOT_CONFIGURATION("org.springframework.boot.SpringBootConfiguration"),

    ;

    public final String name;

    SpringAnnotation(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    public static SpringAnnotation of(String name) {
        for (SpringAnnotation value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
