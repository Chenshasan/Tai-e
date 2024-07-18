package pascal.taie.analysis.pta.plugin.cryptomisuse;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EntryMethodFinder {

    List<List<JMethod>> paths = new ArrayList<>();

    Set<JMethod> visitedMethods = Sets.newSet();

    CallGraph<Invoke, JMethod> callGraph;

    public EntryMethodFinder(CallGraph<Invoke, JMethod> callGraph) {
        this.callGraph = callGraph;
    }

    public void findAllPaths(JMethod start) {
        List<JMethod> path = new ArrayList<>();
        dfs(start, path, paths);
    }

    private void dfs(JMethod currentNode, List<JMethod> path, List<List<JMethod>> paths) {
        path.add(currentNode);
        visitedMethods.add(currentNode);

        if (callGraph.getCallersOf(currentNode).size() == 0) {
            paths.add(new ArrayList<>(path));
        } else {
            for (JMethod neighbor : callGraph.getCallersOf(currentNode)
                    .stream()
                    .map(Invoke::getContainer)
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
        for (List<JMethod> path : paths) {
            JMethod jMethod = path.get(path.size() - 1);
            if(!jMethod.toString().startsWith("<java")){
                entryMethods.add(jMethod);
            }
        }
        return entryMethods;
    }

}
