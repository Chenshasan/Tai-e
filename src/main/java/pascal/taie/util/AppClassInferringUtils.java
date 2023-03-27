package pascal.taie.util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AppClassInferringUtils {

    private AppClassInferringUtils() {
    }

    public static List<String> getAllAppClasses(List<String> appClasses,
                                                List<String> jarPaths) {
        List<String> results = new ArrayList<>(appClasses);
        String appPackagePrefix = inferAppPackagePrefix(appClasses);
        if (appPackagePrefix != null) {
            jarPaths.stream()
                    .map(DirectoryTraverser::listClassesInJar)
                    .flatMap(Collection::stream)
                    .filter(s -> s.startsWith(appPackagePrefix))
                    .forEach(results::add);
        }
        return results;
    }

    public static Collection<String> inferAppJarPaths(Collection<String> appClasses,
                                                      Collection<String> jarPaths) {
        List<String> results = new ArrayList<>();
        String appPackagePrefix = inferAppPackagePrefix(appClasses);
        if (appPackagePrefix != null) {
            for (String jarPath : jarPaths) {
                Collection<String> classes = DirectoryTraverser.listClassesInJar(jarPath);
                long appClassNum = classes.stream().filter(c -> c.startsWith(appPackagePrefix)).count();
                if (appClassNum > classes.size() / 2) {
                    results.add(jarPath);
                }
            }
        }
        return results;
    }

    @Nullable
    public static String inferAppPackagePrefix(Collection<String> appClasses) {
        return appClasses
                .stream()
                .map(s -> {
                    int i = s.indexOf('.');
                    if (i != -1) {
                        int j = s.indexOf('.', i + 1);
                        if (j != -1) {
                            return s.substring(0, j);
                        }
                    }
                    return s;
                })
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

}
