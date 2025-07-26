import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import helpers.ReadFileHelper;
import lombok.extern.slf4j.Slf4j;
import models.graph.GraphSortResult;
import models.profile.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static helpers.LoggerHelper.logProfileDurationMaxInfo;
import static helpers.PropertyHelper.getStepPace;

@Slf4j
public class GeneratePregenerationProfile {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String profilePath = System.getProperty("PROFILE_PATH", "./profiles/test_profile.json");
        String pregenProfileName = System.getProperty("PREGEN_PROFILE_NAME", "pregen_profile");
        double maxTps = Double.parseDouble(System.getProperty("MAX_TPS", "5.0"));

        // Load profile data (canvas or regular)
        TestsParam testsParam;
        String rawProfile = ReadFileHelper.readProfile(profilePath);
        if (profilePath.contains("canvas")) {
            Canvas canvas = new Gson().fromJson(rawProfile, Canvas.class);
            testsParam = new TestsParam();
            testsParam.setTestParam(canvas.getElement().stream().map(Elements::getTestParam).collect(Collectors.toList()));
            testsParam.setCommonSettings(canvas.getCommonSettings());
        } else {
            testsParam = new Gson().fromJson(rawProfile, TestsParam.class);
        }

        double percentProfile = testsParam.getCommonSettings().getBuildSettings().getPercentProfile();
        List<TestParam> testParams = testsParam.getTestParam();

        // 1. Map REDIS_KEY_READ -> REDIS_KEY_ADD (non-null, different)
        Map<String, String> readToAddMap = testParams.stream()
                .map(TestParam::getProperties)
                .filter(Objects::nonNull)
                .filter(p -> p.containsKey("REDIS_KEY_READ") && p.containsKey("REDIS_KEY_ADD"))
                .filter(p -> !p.get("REDIS_KEY_READ").equals(p.get("REDIS_KEY_ADD")))
                .collect(Collectors.toMap(
                        p -> p.get("REDIS_KEY_READ").toString(),
                        p -> p.get("REDIS_KEY_ADD").toString(),
                        (v1, v2) -> v1
                ));

        // 2. Compute sums for each REDIS_KEY_READ
        Map<String, Long> testData = testParams.stream()
                .collect(Collectors.toMap(
                        tp -> tp.getProperties().get("REDIS_KEY_READ").toString(),
                        tp -> {
                            Object readVal = tp.getProperties().get("REDIS_KEY_READ");
                            Object addVal = tp.getProperties().get("REDIS_KEY_ADD");
                            if (readVal.equals(addVal)) return 100L;
                            return tp.getProfiles().stream()
                                    .flatMapToLong(profile -> profile.getSteps().stream()
                                            .mapToLong(step -> {
                                                double profileTps = step.getTps() * percentProfile / 100;
                                                long pacing = getStepPace(profileTps, profile.getPacing());
                                                long stepUsers = (long) Math.ceil(step.getTps() * pacing);
                                                double validTps = 1.0 / (pacing / (double) stepUsers);
                                                return (long) Math.ceil(validTps * (step.getRampTime() + step.getHoldTime()) * 60);
                                            }))
                                    .sum();
                        },
                        (e, r) -> e <= 100 ? e + 100 : e + r
                ));

        // Add minimum threshold and 5% overhead in one pass
        testData.replaceAll((k, v) -> v < 100 ? (long)((v + 100) * 1.05) : (long)(v * 1.05));

        // 3. Accumulate sums following chains using stack
        TreeMap<String, Long> finalData = testData.keySet().stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> {
                            long sum = 0;
                            Set<String> visited = new HashSet<>();
                            Deque<String> stack = new ArrayDeque<>(Collections.singleton(key));
                            while (!stack.isEmpty()) {
                                String curr = stack.pop();
                                if (visited.add(curr)) {
                                    sum += testData.getOrDefault(curr, 0L);
                                    String next = readToAddMap.get(curr);
                                    if (next != null && !visited.contains(next)) stack.push(next);
                                }
                            }
                            return sum;
                        },
                        (v1, v2) -> v1,
                        TreeMap::new
                ));

        finalData.keySet().removeIf(key -> key.contains("mdm"));
        long totalSum = finalData.values().stream().mapToLong(Long::longValue).sum();

        // 4. Build dependency graph
        Map<String, Set<String>> dependencyGraph = testParams.stream()
                .map(TestParam::getProperties)
                .filter(p -> p != null && p.containsKey("REDIS_KEY_READ") && p.containsKey("REDIS_KEY_ADD"))
                .filter(p -> !p.get("REDIS_KEY_READ").equals(p.get("REDIS_KEY_ADD")))
                .collect(Collectors.groupingBy(
                        p -> p.get("REDIS_KEY_ADD").toString(),
                        Collectors.mapping(p -> p.get("REDIS_KEY_READ").toString(), Collectors.toSet())
                ));

        // 5. DFS for topological sort and depth calculation
        GraphSortResult sortedKeys = dfs(finalData.keySet(), dependencyGraph);

        // 6. Map REDIS_KEY_ADD -> TestParam
        Map<Object, TestParam> addKeyToProfile = testParams.stream()
                .filter(tp -> tp.getRun() != null && tp.getProperties() != null)
                .filter(tp -> {
                    Object add = tp.getProperties().get("REDIS_KEY_ADD");
                    Object read = tp.getProperties().get("REDIS_KEY_READ");
                    return add != null && !add.equals(read);
                })
                .collect(Collectors.toMap(
                        tp -> tp.getProperties().get("REDIS_KEY_ADD"),
                        tp -> tp,
                        (v1, v2) -> v1
                ));

        // 7. Build pregeneration profile with reversed keys
        List<String> keysReversed = new ArrayList<>(sortedKeys.getSortedKeys());
        Collections.reverse(keysReversed);
        List<TestParam> pregenProfile = new ArrayList<>();

        for (String key : keysReversed) {
            TestParam tp = addKeyToProfile.get(key);
            if (tp == null) continue;

            double stepTps = maxTps * finalData.get(key) / (double) totalSum;
            int depth = sortedKeys.getDepthMap().getOrDefault(key, 1);

            for (Profile profile : tp.getProfiles()) {
                long pacing = getStepPace(stepTps, profile.getPacing());
                long stepUsers = (long) Math.ceil(stepTps * pacing);
                double validTps = 1.0 / (pacing / (double) stepUsers);
                double holdTime = finalData.get(key) / stepTps / 60.0;

                profile.setSteps(new ArrayList<>(List.of(
                        new Step(0.0, 0.0, 0.5 * (depth - 1)),
                        new Step(validTps, stepUsers / 0.5 / 60 / 1000.0, holdTime)
                )));
            }
            pregenProfile.add(tp);
        }

        testsParam.setTestParam(pregenProfile);
        testsParam.getCommonSettings().getBuildSettings().setPercentProfile(100.0);

        // 8. Find longest scenario duration
        Profile maxDurationProfile = testsParam.getTestParam().stream()
                .flatMap(tp -> tp.getProfiles().stream())
                .max(Comparator.comparingDouble(p -> p.getSteps().stream().mapToDouble(s -> s.getHoldTime() + s.getRampTime()).sum()))
                .orElse(null);

        log.info("\n\n\n");
        finalData.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> log.info(String.format("| %-80.80s | %-30.30s | %-47.47s |\n", e.getKey(), e.getValue(), "~" + testData.get(e.getKey()))));
        log.info("\n\n");

        logProfileDurationMaxInfo(maxDurationProfile);

        try (FileWriter writer = new FileWriter("./" + pregenProfileName + ".json", StandardCharsets.UTF_8)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(testsParam));
            log.info("File Saved Successfully (./{}.json)", pregenProfileName);
        } catch (IOException e) {
            log.error("Error Save Profile: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        log.info("Pregeneration Profile:");
        log.info(gson.toJson(testsParam));
    }

    // Recursive DFS for topological sort & depth calculation with cycle detection
    private static GraphSortResult dfs(Set<String> keys, Map<String, Set<String>> graph) {
        List<String> sortedKeys = new ArrayList<>();
        Map<String, Integer> depthMap = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String key : keys) {
            if (!visited.contains(key)) visit(key, graph, visited, visiting, depthMap, sortedKeys);
        }
        Collections.reverse(sortedKeys);
        return new GraphSortResult(sortedKeys, depthMap);
    }

    private static int visit(String node, Map<String, Set<String>> graph, Set<String> visited,
                             Set<String> visiting, Map<String, Integer> depthMap, List<String> sortedKeys) {
        if (visited.contains(node)) return depthMap.get(node);
        if (visiting.contains(node)) throw new IllegalStateException("Cycle detected at " + node);

        visiting.add(node);
        int maxDepth = graph.getOrDefault(node, Collections.emptySet()).stream()
                .mapToInt(dep -> visit(dep, graph, visited, visiting, depthMap, sortedKeys))
                .max().orElse(0);
        visiting.remove(node);
        visited.add(node);

        int nodeDepth = maxDepth + (!graph.containsKey(node) || graph.get(node).isEmpty() ? 0 : 1);
        depthMap.put(node, nodeDepth);
        sortedKeys.add(node);
        return nodeDepth;
    }
}
