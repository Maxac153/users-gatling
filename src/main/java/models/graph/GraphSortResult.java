package models.graph;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class GraphSortResult {
    private final List<String> sortedKeys;
    private final Map<String, Integer> depthMap;
}
