package pascal.taie.analysis.pta.rpc;

import static pascal.taie.analysis.pta.rpc.BenchmarkConfigs.CRYPTO_BENCHMARKS_DIR;

public enum Benchmark {

    /* Microservice application based on SpringBoot + Dubbo */

    ALIYUN("aliyun-oss-java-sdk", CRYPTO_BENCHMARKS_DIR + "/aliyun-oss-java-sdk"),
    FASTBOOT("fast-boot-weixin", CRYPTO_BENCHMARKS_DIR + "/fast-boot-weixin"),
    SPRINGQUICK("spring-boot-quick", CRYPTO_BENCHMARKS_DIR + "/spring-boot-quick"),

    TELEGRAM("telegram-serverk", CRYPTO_BENCHMARKS_DIR + "/telegram-server"),

    SPRINGSTUDENT("spring-boot-student", CRYPTO_BENCHMARKS_DIR + "/spring-boot-student"),
    XMALL("xmall", CRYPTO_BENCHMARKS_DIR + "/xmall"),
    YUDAO("yudao", CRYPTO_BENCHMARKS_DIR + "/yudao"),
    MOGU("mogu", CRYPTO_BENCHMARKS_DIR + "/mogu"),
    GRUUL("mogu", CRYPTO_BENCHMARKS_DIR + "/gruul"),

    /* Microservice application based on SpringBoot + Feign */

    BASEMALL("basemall", CRYPTO_BENCHMARKS_DIR + "/basemall"),
    YOULAI("youlai", CRYPTO_BENCHMARKS_DIR + "/youlai"),
    NOVEL("novel", CRYPTO_BENCHMARKS_DIR + "/novel"),
    SDUOJ("sduoj", CRYPTO_BENCHMARKS_DIR + "/sduoj"),
    RONCOO("roncoo", CRYPTO_BENCHMARKS_DIR + "/roncoo"),
    MALL4CLOUD("mall4cloud", CRYPTO_BENCHMARKS_DIR + "/mall4cloud"),

    /* other */

    TRAIN_TICKET("train-ticket", CRYPTO_BENCHMARKS_DIR + "/train-ticket"),

    ;

    public final String name;

    public final String dir;


    Benchmark(String name, String dir) {
        this.name = name;
        this.dir = dir;
    }
}
