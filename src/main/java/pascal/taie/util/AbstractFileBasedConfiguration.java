package pascal.taie.util;


import pascal.taie.util.collection.Sets;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

public abstract class AbstractFileBasedConfiguration {

    protected final String classPath;

    protected final Collection<String> concernedKeys;

    protected AbstractFileBasedConfiguration(String classPath) {
        this.classPath = classPath;
        this.concernedKeys = Sets.newSet();
    }

    protected abstract String[] getAcceptableExtensions();

    protected boolean isAcceptableExtension(String filename) {
        for (String acceptableExtension : getAcceptableExtensions()) {
            if (filename.endsWith(acceptableExtension)) {
                return true;
            }
        }
        return false;
    }

    protected abstract void read(String filename,
                                 InputStream inputStream);

    public void initialize() {
        if (classPath.endsWith(".jar")) {
            DirectoryTraverser.walkJarFile(classPath,
                    this::isAcceptableExtension, this::read);
        } else {
            DirectoryTraverser.walkDirectory(classPath, false,
                    this::isAcceptableExtension, this::read);
        }
    }

    public void addConcernedKeys(String... keys) {
        concernedKeys.addAll(Arrays.asList(keys));
    }

    public boolean isConcernedKey(String key) {
        return concernedKeys.contains(key);
    }
}
