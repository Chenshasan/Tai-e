package pascal.taie.analysis.pta.rpc;

public class BenchmarkConfigs {

    /* Spring Boot Monolithic Application */
    public static final String SPRING_BENCHMARKS = "spring-benchmarks";

    public static final String HALO = SPRING_BENCHMARKS + "/halo/halo-1.4.17.jar";

    public static final String PYBBS = SPRING_BENCHMARKS + "/pybbs/pybbs.jar";

    /* micro benchmarks */

    public static final String MICRO_DIR = "../microservice-benchmarks/micro-benchmarks";

    public static final String IOC = MICRO_DIR + "/spring-boot/ioc-1.0.0.jar";
    public static final String WEB = MICRO_DIR + "/spring-boot/web-1.0.0.jar";
    public static final String DUBBO_IOC = MICRO_DIR + "/dubbo/dubbo-ioc-single-1.0.0.jar";
    public static final String FEIGN_IOC = MICRO_DIR + "/feign/fegin-single-1.0.0.jar";
    public static final String MQ = MICRO_DIR + "/mq/rabbitmq-single-1.0.0.jar";

    public static final String CONSUMER_PROVIDER_IOC = MICRO_DIR + "/dubbo/consumer-and-provider-ioc";
    public static final String CONSUMER_PROVIDER_WEB_XML = MICRO_DIR + "/dubbo/consumer-and-provider-web-by-xml";

    public static final String FEIGN_CONSUMER_PROVIDER_WEB = MICRO_DIR + "/feign/consumer-and-provider-web";

    public static final String MQ_CONSUMER_PROVIDER = MICRO_DIR + "/mq/rabbitmq-consumer-and-provider";

    public static final String REST_TWO_SERVICES = MICRO_DIR + "/rest/two-services";


    /* real world benchmarks */

    public static final String CRYPTO_BENCHMARKS_DIR = "crypto-benchmarks";
}
