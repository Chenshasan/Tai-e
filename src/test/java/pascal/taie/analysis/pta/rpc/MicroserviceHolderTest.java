package pascal.taie.analysis.pta.rpc;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.util.AppClassInferringUtils;
import pascal.taie.util.DirectoryTraverser;
import pascal.taie.util.ZipUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class MicroserviceHolderTest {

    /* Micro-benchmarks */

    //@Test
    //public void ioc() {
    //    Tests.testPTABySpringBootJar(BenchmarkConfigs.IOC, false);
    //}
    //
    //@Test
    //public void dubboIoc() {
    //    Tests.testPTABySpringBootJar(BenchmarkConfigs.DUBBO_IOC, false);
    //}
    //
    //@Test
    //public void dubboConsumerAndProviderIoc() {
    //    Tests.testPTABySpringBootArchives(BenchmarkConfigs.CONSUMER_PROVIDER_IOC, false);
    //}
    //
    //@Test
    //public void dubboWeb() {
    //    Tests.testPTABySpringBootArchives(BenchmarkConfigs.CONSUMER_PROVIDER_WEB_XML, false);
    //}
    //
    //@Test
    //public void feignIoc() {
    //    Tests.testPTABySpringBootJar(BenchmarkConfigs.FEIGN_IOC, false);
    //}
    //
    //@Test
    //public void feignWeb() {
    //    Tests.testPTABySpringBootArchives(BenchmarkConfigs.FEIGN_CONSUMER_PROVIDER_WEB, false);
    //}
    //
    //@Test
    //public void mq() {
    //    Tests.testPTABySpringBootJar(BenchmarkConfigs.MQ, true);
    //}
    //
    //@Test
    //public void mqConsumerAndProvider() {
    //    Tests.testPTABySpringBootArchives(BenchmarkConfigs.MQ_CONSUMER_PROVIDER, true);
    //}
    //
    //@Test
    //public void restTwoServices() {
    //    Tests.testPTABySpringBootArchives(BenchmarkConfigs.REST_TWO_SERVICES, true);
    //}

    /* Spring Booot Monolithic Application */
    //@Test
    //public void halo() {
    //    Tests.testPTABySpringBootJar(BenchmarkConfigs.HALO, true);
    //}
    //
    //@Test
    //public void pybbs() {
    //    Tests.testPTABySpringBootJar(BenchmarkConfigs.PYBBS, true);
    //}

    /* Microservice application based on SpringBoot + Dubbo */
    @Test
    public void aliyun() {
        Tests.testPTABySpringBootArchives(Benchmark.ALIYUN, true);
    }

    @Test
    public void fastBoot() {
        Tests.testPTABySpringBootArchivesOfCrypto(Benchmark.FASTBOOT, true);
    }

    @Test
    public void springQuick() {
        Tests.testPTABySpringBootArchivesOfCrypto(Benchmark.SPRINGQUICK, true);
    }

    @Test
    public void springSTUDENT() {
        Tests.testPTABySpringBootArchivesOfCrypto(Benchmark.SPRINGSTUDENT, true);
    }

    @Test
    public void telegramServer() {
        Tests.testPTAInLibraryProgramOfCrypto(Benchmark.TELEGRAM, true);
    }


    @Test
    public void gruul() {
        Tests.testPTABySpringBootArchives(Benchmark.GRUUL, true);
    }

    @Test
    public void xmall() {
        Tests.testPTABySpringBootArchives(Benchmark.XMALL, true);
    }

    /* Microservice application based on SpringBoot + Feign */

    @Test
    public void mogu() {
        Tests.testPTABySpringBootArchives(Benchmark.MOGU, true);
    }

    @Test
    public void basemall() {
        Tests.testPTABySpringBootArchives(Benchmark.BASEMALL, true);
    }

    @Test
    public void youlai() {
        Tests.testPTABySpringBootArchives(Benchmark.YOULAI, true);
    }

    @Test
    public void novel() {
        Tests.testPTABySpringBootArchives(Benchmark.NOVEL, true);
    }

    @Test
    public void roncoo() {
        Tests.testPTABySpringBootArchives(Benchmark.RONCOO, true);
    }

    @Test
    public void sduoj() {
        Tests.testPTABySpringBootArchives(Benchmark.SDUOJ, true);
//        Tests.testMicroserviceBenchmarkViaYaml("sduoj");
    }

    @Test
    public void mall4cloud() {
        Tests.testPTABySpringBootArchives(Benchmark.MALL4CLOUD, true);
    }

    @Test
    public void trainTicket() {
        Tests.testPTABySpringBootArchives(Benchmark.TRAIN_TICKET, true);
    }

    /**
     * uncompress Spring archives and construct app-info.yml
     */
    //@Test
    public void uncompressSpringArchives() throws IOException {
        Path uncompressDir = Path.of("uncompressed-microservice-benchmarks").toAbsolutePath();
        StringBuilder sb = new StringBuilder();
        for (Benchmark benchmark : Benchmark.values()) {
            Collection<String> appJarPaths = Sets.newSet();
            Collection<String> libJarPaths;
            Map<String, String> libName2LibJarPath = Maps.newMap();
            // uncompress jar and war archives
            File benchmarkDir = new File(benchmark.dir);
            for (File archive : Objects.requireNonNull(benchmarkDir.listFiles())) {
                String archiveName = archive.getName();
                if (archiveName.endsWith(".jar") || archiveName.endsWith(".war")) {
                    // uncompress archive
                    Path targetDir = uncompressDir.resolve(benchmarkDir.getName())
                                                  .resolve(archiveName.substring(0, archiveName.lastIndexOf(".")));
                    targetDir.toFile().mkdirs();
                    ZipUtils.uncompressZipFile(archive.getAbsolutePath(), targetDir.toFile().getAbsolutePath());
                    // construct class path
                    Path inf = targetDir.resolve("BOOT-INF");
                    if (!inf.toFile().exists()) {
                        inf = targetDir.resolve("WEB-INF");
                    }
                    // lib class path
                    Path lib = inf.resolve("lib");
                    for (File file : Objects.requireNonNull(lib.toFile().listFiles())) {
                        if (file.getName().endsWith(".jar")) {
                            libName2LibJarPath.put(file.getName(), file.getAbsolutePath());
                        }
                    }
                    // compress app classes to jar
                    Path classes = inf.resolve("classes");
                    Path jarFile = classes.resolveSibling(classes.getFileName() + ".jar");
                    ZipUtils.compressDirectory(classes, jarFile, false);
                    removeDirectory(classes);
                    appJarPaths.add(jarFile.toFile().getAbsolutePath());
                }
            }
            libJarPaths = new HashSet<>(libName2LibJarPath.values());
            // infer the actual app jar paths
            List<String> appClasses = appJarPaths.stream()
                                                 .map(DirectoryTraverser::listClassesInJar)
                                                 .flatMap(Collection::stream)
                                                 .toList();
            Collection<String> inferredAppJarPaths = AppClassInferringUtils.inferAppJarPaths(
                    appClasses, libJarPaths);
            appJarPaths.addAll(inferredAppJarPaths);
            libJarPaths.removeAll(inferredAppJarPaths);
            // change to relative path
            appJarPaths = appJarPaths.stream()
                                     .map(Path::of)
                                     .map(uncompressDir::relativize)
                                     .map(Path::toString)
                                     .sorted()
                                     .toList();
            libJarPaths = libJarPaths.stream()
                                     .map(Path::of)
                                     .map(uncompressDir::relativize)
                                     .map(Path::toString)
                                     .sorted()
                                     .toList();
            // output app-info
            sb.append(benchmarkDir.getName()).append(":\n");
            sb.append("  apps:\n");
            for (String appClassPath : appJarPaths) {
                sb.append("    - \"").append(appClassPath.replace('\\', '/')).append("\"\n");
            }
            sb.append("  libs:\n");
            for (String value : libJarPaths) {
                sb.append("    - \"").append(value.replace('\\', '/')).append("\"\n");
            }
            sb.append("\n");
        }
        Files.writeString(uncompressDir.resolve("app-info.yml"), sb.toString());
    }

    public static void removeDirectory(Path dir) throws IOException {
        Files.walk(dir)
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }
}
