import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import helpers.ReadFileHelper;
import models.graph.GraphSortResult;
import models.profile.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static helpers.LoggerHelper.logProfileDurationMaxInfo;
import static helpers.PropertyHelper.getStepPace;

public class GeneratePregenerationProfile {
    private static final Logger log = LoggerFactory.getLogger(GeneratePregenerationProfile.class);
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String profilePath = System.getProperty("PROFILE_PATH", "./profiles/test_profile.json");
        String pregenProfileName = System.getProperty("PREGEN_PROFILE_NAME", "pregen_profile");
        double maxTps = Double.parseDouble(System.getProperty("MAX_TPS", "100.0"));

        TestsParam testsParam = new TestsParam();
        String testsParamString = ReadFileHelper.read(profilePath);
        if (profilePath.contains("canvas")) {
            Canvas testsParamCanvas = new Gson().fromJson(testsParamString, Canvas.class);
            List<TestParam> testParams = new ArrayList<>();

            for (Elements testParam : testsParamCanvas.getElement()) {
                testParams.add(testParam.getTestParam());
            }

            testsParam.setTestParam(testParams);
            testsParam.setCommonSettings(testsParamCanvas.getCommonSettings());
        } else {
            testsParam = new Gson().fromJson(testsParamString, TestsParam.class);
        }

        Double percentProfile = testsParam.getCommonSettings().getBuildSettings().getPercentProfile();
        List<TestParam> testParams = testsParam.getTestParam();

        // 1. Map REDIS_KEY_READ к REDIS_KEY_ADD (non-null)
        Map<String, String> readToAddMap = testParams.stream()
                .map(TestParam::getProperties)
                .filter(Objects::nonNull)
                .filter(props -> Objects.nonNull(props.get("REDIS_KEY_READ").toString()) &&
                                 Objects.nonNull(props.get("REDIS_KEY_ADD").toString()) &&
                                 !props.get("REDIS_KEY_READ").toString().equals(props.get("REDIS_KEY_ADD").toString())
                )
                .collect(Collectors.toMap(
                        props -> props.get("REDIS_KEY_READ").toString(),
                        props -> props.get("REDIS_KEY_ADD").toString(),
                        (v1, v2) -> v1,
                        HashMap::new
                ));

        // 2. Считаем сумму для каждого REDIS_KEY_READ
        Map<String, Long> testData = testParams.stream()
                .collect(Collectors.toMap(
                        testParam -> testParam.getProperties().get("REDIS_KEY_READ").toString(),
                        testParam -> {
                            Object redisKeyAdd = testParam.getProperties().get("REDIS_KEY_ADD");
                            Object redisKeyRead = testParam.getProperties().get("REDIS_KEY_READ");

                            if (redisKeyRead != null && redisKeyRead.equals(redisKeyAdd)) {
                                return 100L;
                            } else {
                                return testParam.getProfiles().stream()
                                        .flatMapToLong(profile -> profile.getSteps().stream()
                                                .mapToLong(step -> {
                                                    double profileTps = step.getTps() * percentProfile / 100;

                                                    long pacing = getStepPace(profileTps, profile.getPacing());
                                                    long stepUsers = (long) Math.ceil(step.getTps() * pacing);
                                                    double validTps = 1 / (pacing / (double) stepUsers);

                                                    return (long) Math.ceil(validTps * (step.getRampTime() + step.getHoldTime()) * 60);
                                                })
                                        ).sum();
                            }
                        },
                        (existing, replacement) -> {
                            if (existing <= 100) {
                                return existing + 100;
                            } else {
                                return existing + replacement;
                            }
                        },
                        HashMap::new
                ));

        testData.entrySet().forEach(entry -> {
            if (entry.getValue() < 100) {
                entry.setValue(entry.getValue() + 100);
            }
            entry.setValue((long) (entry.getValue() * 1.05));
        });

        // 3. Аккумулируем сумму, следуя цепочкам ключей
        TreeMap<String, Long> finalData = testData.keySet().stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> {
                            Set<String> visited = new HashSet<>();
                            Deque<String> stack = new ArrayDeque<>();

                            stack.push(key);
                            long totalSum = 0;

                            while (!stack.isEmpty()) {
                                String current = stack.poll();
                                if (!visited.add(current)) {
                                    continue;
                                }
                                totalSum += testData.getOrDefault(current, 0L);
                                String next = readToAddMap.get(current);
                                if (next != null && !visited.contains(next)) {
                                    stack.push(next);
                                }
                            }
                            return totalSum;
                        },
                        (v1, v2) -> v1,
                        TreeMap::new
                ));

        // Удаляем все ключи в которых есть слово mdm (преген из базы в редис делаем отдельно)
        finalData.keySet().removeIf(key -> key.contains("mdm"));
        long total = finalData.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        // 4. Собираем граф: REDIS_KEY_ADD зависит от REDIS_KEY_READ
        Map<String, Set<String>> dependencyGraph = testParams.stream()
                .map(TestParam::getProperties)
                .filter(Objects::nonNull)
                .filter(props -> Objects.nonNull(props.get("REDIS_KEY_READ")) &&
                                 Objects.nonNull(props.get("READIS_KEY_ADD")) &&
                                 !props.get("REDIS_KEY_READ").toString().equals(props.get("REDIS_KEY_ADD").toString()))
                .collect(Collectors.groupingBy(
                        props -> props.get("REDIS_KEY_ADD").toString(),
                        Collectors.mapping(props -> props.get("REDIS_KEY_READ").toString(),
                                Collectors.toCollection(HashSet::new))
                ));

        // 5. Топологическая сортировка ключей
        GraphSortResult sortedKeys = dfs(finalData.keySet(), dependencyGraph);

        // 6. Map REDIS_KEY_ADD to TestsParam
        HashMap<Object, TestParam> addKeyToProfile = testParams.stream()
                .filter(tp -> Objects.nonNull(tp.getRun()) &&
                              Objects.nonNull(tp.getProperties()) &&
                              Objects.nonNull(tp.getProperties().get("REDIS_KEY_ADD")) &&
                              !Objects.equals(tp.getProperties().get("REDIS_KEY_ADD"),
                                      tp.getProperties().get("REDIS_KEY_READ")))
                .collect(Collectors.toMap(
                        tp -> tp.getProperties().get("REDIS_KEY_ADD"),
                        tp -> tp,
                        (v1, v2) -> v1,
                        HashMap::new
                ));

        // 7. Сборка профиля для прегенерации данных
        List<String> reversedKeys = new ArrayList<>(sortedKeys.getSortedKeys());
        Collections.reverse(reversedKeys);
        ArrayList<TestParam> pregenProfile = new ArrayList<>();

        for (String key : reversedKeys) {
            TestParam testParam = addKeyToProfile.get(key);
            if (testParam == null) {
                continue;
            }

            double stepTps = maxTps * finalData.get(key) / (double) total;
            for (Profile profile : testParam.getProfiles()) {
                long pacing = getStepPace(stepTps, profile.getPacing());
                long stepUsers = (long) Math.ceil(stepTps * pacing);
                double validTps = 1 / (pacing / (double) stepUsers);
                double holdTime = finalData.get(key) / stepTps / 60.0;

                ArrayList<Step> steps = new ArrayList<>();
                steps.add(new Step(0.0, 0.0, 0.5 * (sortedKeys.getDepthMap().get(key) - 1)));
                // TODO Может быть потенциальной ошибкой! (Попробовать validTps поменять на tps)
                steps.add(new Step(validTps, (double) stepUsers / 0.5 / 60 / 1000, holdTime));

                profile.setSteps(steps);
            }

            pregenProfile.add(testParam);
        }

        testsParam.setTestParam(pregenProfile);
        testsParam.getCommonSettings().getBuildSettings().setPercentProfile(100.0);
        String pregenProfileStr = new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(testsParam);

        // 8. Поиск самого долгого сценария
        Profile scenarioDurationMax = testsParam.getTestParam().stream()
                .flatMap(testParam -> testParam.getProfiles().stream())
                .max(Comparator.comparingDouble(profile ->
                        profile.getSteps().stream()
                                .mapToDouble(step -> step.getHoldTime() + step.getRampTime())
                                .sum()
                ))
                .orElse(null);


        log.info("");
        log.info("");
        log.info("");
        finalData.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    log.info(String.format("| %-80.80s | %-30.30s | %-47.47s |", entry.getKey(), entry.getValue(), "~" + testData.get(entry.getKey())));
                    log.info("");
                });
        log.info(String.format(""));
        log.info("");

        logProfileDurationMaxInfo(scenarioDurationMax);

        try (FileWriter writer = new FileWriter("./" + pregenProfileName + ".json", StandardCharsets.UTF_8)) {
            writer.write(pregenProfileStr);
            log.info("File Saved Successfully (./{}.json)", pregenProfileName);
        } catch (IOException e) {
            log.error("Error Save Profile: " + e.getMessage());
            throw new RuntimeException(e);
        }

        log.info("Pregeneration Profile:");
        log.info(gson.toJson(testsParam));
    }

    private static GraphSortResult dfs(
            Set<String> keys,
            Map<String, Set<String>> dependencyGraph
    ) {
        List<String> sortedKeys = new ArrayList<>();
        Map<String, Integer> depthMap = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        for (String key : keys) {
            if (!visited.contains(key)) {
                stack.push(key);
                while (!stack.isEmpty()) {
                    String current = stack.peek();

                    if (visiting.contains(current)) {
                        stack.pop();
                        visiting.remove(current);
                        visited.add(current);
                        sortedKeys.add(current);

                        Set<String> dependencies = dependencyGraph.getOrDefault(current, Collections.emptySet());
                        int maxDepDepth = 0;
                        for (String dep : dependencies) {
                            maxDepDepth = Math.max(maxDepDepth, depthMap.getOrDefault(dep, 0));
                        }
                        depthMap.put(current, dependencies.isEmpty() ? 0 : maxDepDepth + 1);
                        continue;
                    }
                    if (visited.contains(current)) {
                        stack.pop();
                        continue;
                    }

                    visiting.add(current);
                    Set<String> dependencies = dependencyGraph.getOrDefault(current, Collections.emptySet());
                    boolean hasUnvisited = false;
                    for (String dep : dependencies) {
                        if (!visited.contains(dep)) {
                            stack.push(dep);
                            hasUnvisited = true;
                        }
                    }
                    if (!hasUnvisited && dependencies.isEmpty()) {
                        // Лист — глубина 0
                        stack.pop();
                        visiting.remove(current);
                        visited.add(current);
                        sortedKeys.add(current);
                        depthMap.put(current, 0);
                    }
                }
            }
        }
        Collections.reverse(sortedKeys);
        return new GraphSortResult(sortedKeys, depthMap);
    }
}
