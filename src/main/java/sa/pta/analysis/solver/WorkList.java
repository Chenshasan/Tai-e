package sa.pta.analysis.solver;

import sa.callgraph.Edge;
import sa.pta.analysis.data.CSCallSite;
import sa.pta.analysis.data.CSMethod;
import sa.pta.analysis.data.Pointer;
import sa.pta.set.PointsToSet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

class WorkList {

    private Queue<Entry> pointerEntries = new LinkedList<>();

    private Set<Edge<CSCallSite, CSMethod>> edges = new TreeSet<>();

    boolean hasPointerEntries() {
        return !pointerEntries.isEmpty();
    }

    void addPointerEntry(Pointer pointer, PointsToSet pointsToSet) {
        addPointerEntry(new Entry(pointer, pointsToSet));
    }

    void addPointerEntry(Entry entry) {
        pointerEntries.add(entry);
    }

    Entry pollPointerEntry() {
        return pointerEntries.poll();
    }

    class Entry {

        final Pointer pointer;

        final PointsToSet pointsToSet;

        public Entry(Pointer pointer, PointsToSet pointsToSet) {
            this.pointer = pointer;
            this.pointsToSet = pointsToSet;
        }
    }

    void addEdge(Edge<CSCallSite, CSMethod> edge) {
        edges.add(edge);
    }

    Edge<CSCallSite, CSMethod> pollEdge() {
        Edge<CSCallSite, CSMethod> edge = edges.iterator().next();
        edges.remove(edge);
        return edge;
    }
}
