package models.graph;

import java.util.List;
import java.util.Map;

public class GraphSortResult {
    private final List<String> sortedKeys;
    private final Map<String, Integer> depthMap;

    public GraphSortResult(List<String> sortedKeys, Map<String, Integer> depthMap) {
        this.sortedKeys = sortedKeys;
        this.depthMap = depthMap;
    }

    public List<String> getSortedKeys() {
        return sortedKeys;
    }

    public Map<String, Integer> getDepthMap() {
        return depthMap;
    }
}
