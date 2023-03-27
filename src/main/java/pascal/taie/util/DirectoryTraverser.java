package pascal.taie.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * an util for traversing the directory recursively
 * special features: support scanning the Jar file
 * TODO: support the nested Jar file
 */
public class DirectoryTraverser {

    private static final Logger logger = LogManager.getLogger(DirectoryTraverser.class);

    public static void walkDirectory(String directoryPath,
                                     boolean includeJar,
                                     Predicate<String> fileNamePredicate,
                                     BiConsumer<String, InputStream> action) {
        try {
            Files.walk(Path.of(directoryPath)).forEach(filePath -> {
                String filePathStr = filePath.toString();
                if (fileNamePredicate.test(filePathStr)) {
                    try {
                        FileInputStream inputStream = new FileInputStream(filePath.toFile());
                        action.accept(filePathStr, inputStream);
                    } catch (IOException e) {
                        logger.error("get input stream error: " + filePath);
                    }
                }
                if (includeJar && filePathStr.endsWith(".jar")) {
                    walkJarFile(filePath.toAbsolutePath().toString(), fileNamePredicate,
                            action);
                }
            });
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public static void walkJarFile(String jarFilePath,
                                   Predicate<String> fileNamePredicate,
                                   BiConsumer<String, InputStream> action) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(new File(jarFilePath));
        } catch (Exception e) {
            throw new RuntimeException("can not find the Jar file: " + jarFilePath);
        }

        final JarFile finalJarFile = jarFile;
        jarFile.stream().forEach(jarEntry -> {
            String jarEntryName = jarEntry.getName();
            if (!jarEntry.isDirectory()) {
                if (fileNamePredicate.test(jarEntryName)) {
                    try {
                        InputStream inputStream = finalJarFile.getInputStream(jarEntry);
                        action.accept(jarEntryName, inputStream);
                    } catch (IOException e) {
                        logger.error("get input stream error: " + jarEntryName);
                    }
                }
            }
        });
    }

    public static List<String> listClasses(String path) {
        if (path.endsWith(".jar")) {
            return listClassesInJar(path);
        } else {
            return listClassesInDir(path);
        }
    }

    public static List<String> listClassesInDir(String directoryPath) {
        String temp = directoryPath + File.separator;
        try {
            return Files.walk(Path.of(directoryPath))
                        .map(Path::toString)
                        .filter(path -> path.endsWith(".class"))
                        .map(path -> path.replace(temp, "")
                                         .replace(".class", "")
                                         .replace(File.separator, "."))
                        .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("", e);
        }
        return List.of();
    }

    public static List<String> listClassesInJar(String jarFilePath) {
        Set<String> classes = Sets.newSet();
        try (JarFile jarFile = new JarFile(new File(jarFilePath))) {
            jarFile.stream().forEach(jarEntry -> {
                String jarEntryName = jarEntry.getName();
                if (!jarEntry.isDirectory()) {
                    if (jarEntryName.endsWith(".class")) {
                        classes.add(jarEntryName.replace(".class", "")
                                                .replace(File.separator, ".")
                                                .replace("/", "."));
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return classes.stream().toList();
    }
}
