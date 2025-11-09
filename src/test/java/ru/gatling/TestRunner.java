package ru.gatling;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import ru.gatling.helpers.ReadFileHelper;
import ru.gatling.models.profile.Profile;
import ru.gatling.models.profile.TestParam;
import ru.gatling.models.profile.TestsParam;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static ru.gatling.helpers.LoggerHelper.logProfileDurationMaxInfo;
import static ru.gatling.helpers.LoggerHelper.logProfileInfo;

public class TestRunner extends Simulation {
    private final ArrayList<PopulationBuilder> POPULATION_BUILDERS = new ArrayList<>();

    {
        setUp(testRunner());
    }

    private ArrayList<PopulationBuilder> testRunner() throws RuntimeException {
        String profilePath = System.getProperty("PROFILE", "./profiles/test_profile.json");
        TestsParam testsParam = ReadFileHelper.readProfile(profilePath);
        Double percentProfile = testsParam.getCommonSettings().getRunSettings().getPercentProfile();

        testsParam.getTestParam().values().stream()
                .filter(testParam -> testParam.getProfiles() != null)
                .flatMap(testParam -> testParam.getProfiles().stream())
                .flatMap(profile -> profile.getSteps().stream())
                .forEach(step -> step.setTps(step.getTps() * percentProfile / 100.0));

        HashMap<String, Object> commonSettings = testsParam.getCommonSettings().getProperties();
        boolean debugEnable = Boolean.parseBoolean(commonSettings.get("DEBUG_ENABLE").toString());

        Profile scenarioDurationMax = testsParam.getTestParam().values().stream()
                .flatMap(testParam -> testParam.getProfiles().stream())
                .max(Comparator.comparingDouble(profile ->
                        profile.getSteps().stream()
                                .mapToDouble(step -> step.getHoldTime() + step.getRampTime())
                                .sum()
                ))
                .orElse(null);

        for (Map.Entry<String, TestParam> entry : testsParam.getTestParam().entrySet()) {
            String scenarioName = entry.getKey();
            TestParam testParam = entry.getValue();

            try {
                Class<?> classSimulation = Class.forName(testParam.getRun().getSimulationClass());
                Method method = classSimulation.getMethod("run", String.class, Map.class, ArrayList.class);
                Object instance = classSimulation.getDeclaredConstructor().newInstance();

                commonSettings.put("ENV", testParam.getRun().getEnv());

                if (!debugEnable) {
                    for (Profile profile : testParam.getProfiles()) {
                        logProfileInfo(profile);
                    }
                }

                commonSettings.putAll(testParam.getProperties());
                POPULATION_BUILDERS.add((PopulationBuilder) method.invoke(instance, scenarioName, commonSettings, testParam.getProfiles()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        logProfileDurationMaxInfo(scenarioDurationMax);
        return POPULATION_BUILDERS;
    }
}
