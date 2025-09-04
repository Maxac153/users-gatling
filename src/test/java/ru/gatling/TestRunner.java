package ru.gatling;

import com.google.gson.Gson;
import ru.gatling.helpers.ReadFileHelper;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import ru.gatling.models.profile.*;

import java.lang.reflect.Method;
import java.util.*;

import static ru.gatling.helpers.LoggerHelper.logProfileDurationMaxInfo;
import static ru.gatling.helpers.LoggerHelper.logProfileInfo;

public class TestRunner extends Simulation {
    private final List<PopulationBuilder> populationBuilders = new ArrayList<>();

    {
        setUp(testRunner());
    }

    private List<PopulationBuilder> testRunner() throws RuntimeException {
        String profilePath = System.getProperty("PROFILE", "./profiles/test_profile.json");
        String testsParamString = ReadFileHelper.readProfile(profilePath);

        TestsParam testsParam = new TestsParam();
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

        Double percentProfile = testsParam.getCommonSettings().getRunSettings().getPercentProfile();

        testsParam.getTestParam().stream()
                .filter(testParam -> testParam.getProfiles() != null)
                .flatMap(testParam -> testParam.getProfiles().stream())
                .flatMap(profile -> profile.getSteps().stream())
                .forEach(step -> step.setTps(step.getTps() * percentProfile / 100.0));

        HashMap<String, Object> commonSettings = testsParam.getCommonSettings().getProperties();
        boolean debugEnable = Boolean.parseBoolean(commonSettings.get("DEBUG_ENABLE").toString());

        Profile scenarioDurationMax = testsParam.getTestParam().stream()
                .flatMap(testParam -> testParam.getProfiles().stream())
                .max(Comparator.comparingDouble(profile ->
                        profile.getSteps().stream()
                                .mapToDouble(step -> step.getHoldTime() + step.getRampTime())
                                .sum()
                ))
                .orElse(null);

        for (TestParam testParam : testsParam.getTestParam()) {
            try {
                Class<?> classSimulation = Class.forName(testParam.getRun().getSimulationClass());
                Method method = classSimulation.getMethod("run", Map.class, ArrayList.class);
                Object instance = classSimulation.getDeclaredConstructor().newInstance();
                commonSettings.put("ENV", testParam.getRun().getEnv());

                if (!debugEnable) {
                    for (Profile profile : testParam.getProfiles()) {
                        logProfileInfo(profile);
                    }
                }

                commonSettings.putAll(testParam.getProperties());
                populationBuilders.add((PopulationBuilder) method.invoke(instance, commonSettings, testParam.getProfiles()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        logProfileDurationMaxInfo(scenarioDurationMax);
        return populationBuilders;
    }
}
