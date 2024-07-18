package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.pta.core.cs.CSCallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CSEntryMethodFinder {

    List<List<CSMethod>> paths = new ArrayList<>();

    Set<CSMethod> visitedMethods = Sets.newSet();

    CSCallGraph csCallGraph;

    public CSEntryMethodFinder(CSCallGraph csCallGraph) {
        this.csCallGraph = csCallGraph;
    }

    public void findAllPaths(CSMethod start) {
        List<CSMethod> path = new ArrayList<>();
        dfs(start, path, paths);
    }

    private void dfs(CSMethod currentNode, List<CSMethod> path, List<List<CSMethod>> paths) {
        path.add(currentNode);
        visitedMethods.add(currentNode);

        if (csCallGraph.getCallersOf(currentNode).size() == 0) {
            paths.add(new ArrayList<>(path));
        } else {
            for (CSMethod neighbor : csCallGraph.getCallersOf(currentNode)
                    .stream()
                    .map(CSCallSite::getContainer)
                    .collect(Collectors.toSet())) {
                if (!visitedMethods.contains(neighbor)) {
                    dfs(neighbor, path, paths);
                }
            }
        }
        path.remove(path.size() - 1); // backtracking
    }

    public Set<JMethod> getEntryMethods() {
        Set<JMethod> entryMethods = Sets.newSet();
        for (List<CSMethod> path : paths) {
            entryMethods.add(path.get(path.size() - 1).getMethod());
        }
        return entryMethods;
    }
}
