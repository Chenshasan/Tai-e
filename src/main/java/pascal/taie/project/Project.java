package pascal.taie.project;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Representation of a Java project.
 */
public class Project {

    private final String mainClass;

    private final int javaVersion;

    private final List<String> inputClasses;

    private final List<FileContainer> appRootContainers;

    private final List<FileContainer> libRootContainers;

    Project(String mainClass,
            int javaVersion,
            List<String> inputClasses,
            List<FileContainer> appRootContainers,
            List<FileContainer> libRootContainers) {
        this.mainClass = mainClass;
        this.javaVersion = javaVersion;
        this.inputClasses = inputClasses;
        this.appRootContainers = appRootContainers;
        this.libRootContainers = libRootContainers;
    }

    public String getMainClass() {
        return mainClass;
    }

    public int getJavaVersion() {
        return javaVersion;
    }

    public List<String> getInputClasses() {
        return inputClasses;
    }

    public List<FileContainer> getAppRootContainers() {
        return appRootContainers;
    }

    public List<FileContainer> getLibRootContainers() {
        return libRootContainers;
    }

    public boolean isApp(AnalysisFile file) {
        return appRootContainers.contains(file.rootContainer());
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return the first file (with the same fully qualified name) found in the containerLists.
     * (QUESTION: how to define priority between different rootContainers?)
     */
    public AnalysisFile locate(String className) {
        List<List<FileContainer>> rootContainersList =
                List.of(appRootContainers, libRootContainers);

        for (List<FileContainer> rootContainers : rootContainersList) {
            for (FileContainer container : rootContainers) {
                // make sure to keep the order.
                ClassLocation classLocation = new ClassLocation(className);
                assert classLocation.hasNext();
                AnalysisFile result = container.locate(classLocation);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * @param className the fully qualified name to the analysis file.
     * @return all the files with the full path.
     */
    public List<AnalysisFile> locateFiles(String className) {
        List<AnalysisFile> results = new ArrayList<>();

        Consumer<FileContainer> get = c -> {
            ClassLocation classLocation = new ClassLocation(className);
            assert classLocation.hasNext();
            AnalysisFile result = c.locate(classLocation);
            if (result != null) {
                results.add(result);
            }
        };

        appRootContainers.forEach(get);

        libRootContainers.forEach(get);

        return results;
    }
}
