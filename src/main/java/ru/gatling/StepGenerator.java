package ru.gatling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import ru.gatling.helpers.ReadFileHelper;
import ru.gatling.models.profile.Profile;
import ru.gatling.models.profile.Step;
import ru.gatling.models.profile.TestParam;
import ru.gatling.models.profile.TestsParam;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
public class StepGenerator {
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        String profilePath = System.getProperty("PROFILE_PATH", "./profiles/test_profile.json");
        String modifiedProfileName = System.getProperty("MODIFIED_PROFILE_NAME", "modified_profile");
        int numberSteps = Integer.parseInt(System.getProperty("NUMBER_STEPS", "3"));
        double percentageFistStep = Double.parseDouble(System.getProperty("PERCENTAGE_FIRST_STEP", "100.0"));
        double percentageStep = Double.parseDouble(System.getProperty("PERCENTAGE_STEP", "20.0"));
        double rumpTime = Double.parseDouble(System.getProperty("RUMP_TIME", "15.0"));
        double holdTime = Double.parseDouble(System.getProperty("HOLD_TIME", "30.0"));

        TestsParam testsParam = ReadFileHelper.readProfile(profilePath);
        Double percentProfile = testsParam.getCommonSettings().getRunSettings().getPercentProfile();
        HashMap<String, TestParam> testParams = testsParam.getTestParam();

        for (TestParam testParam : testParams.values()) {
            for (Profile profile : testParam.getProfiles()) {
                double tps = 0.0;
                double pause = 0.0;
                ArrayList<Step> steps = new ArrayList<>();

                for (Step step : profile.getSteps()) {
                    if (step.getTps() != 0.0) {
                        tps = step.getTps() * percentProfile / 100;
                        break;
                    } else {
                        pause = step.getRampTime() + step.getHoldTime();
                        steps.add(new Step(0.0, step.getRampTime(), step.getHoldTime()));
                    }
                }

                for (int i = 0; i < numberSteps; i++) {
                    if (i == 0 && pause != 0) {
                        steps.add(new Step(tps * (percentageFistStep) / 100, rumpTime - pause, holdTime));
                    } else {
                        steps.add(new Step(tps * (percentageFistStep + percentageStep * i) / 100, rumpTime, holdTime));
                    }
                }
                profile.setSteps(steps);
            }
        }

        testsParam.getCommonSettings().getRunSettings().setPercentProfile(100.0);
        try (FileWriter writer = new FileWriter("./" + modifiedProfileName + ".json", StandardCharsets.UTF_8)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(testsParam));
            log.info("File Saved Successfully (./{}.json", modifiedProfileName);
        } catch (IOException e) {
            log.error("Error Save Profile: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        log.info("New Profile:");
        log.info(GSON.toJson(testsParam));
    }
}
