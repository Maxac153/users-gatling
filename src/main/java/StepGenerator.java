import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import helpers.ReadFileHelper;
import lombok.extern.slf4j.Slf4j;
import models.profile.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StepGenerator {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String profilePath = System.getProperty("PROFILE_PATH", "./profiles/test_profile.json");
        String modifiedProfileName = System.getProperty("MODIFIED_PROFILE_NAME", "modified_profile");
        int numberSteps = Integer.parseInt(System.getProperty("NUMBER_STEPS", "3"));
        double percentageFistStep = Double.parseDouble(System.getProperty("PERCENTAGE_FIRST_STEP", "100.0"));
        double percentageStep = Double.parseDouble(System.getProperty("PERCENTAGE_STEP", "20.0"));
        double rumpTime = Double.parseDouble(System.getProperty("RUMP_TIME", "15.0"));
        double holdTime = Double.parseDouble(System.getProperty("HOLD_TIME", "30.0"));

        TestsParam testsParam = new TestsParam();
        String testsParamString = ReadFileHelper.read(profilePath);

        if (profilePath.contains("canvas")) {
            Canvas testsParamCanvas = gson.fromJson(testsParamString, Canvas.class);
            List<TestParam> testParams = new ArrayList<>();

            for (Elements testParam : testsParamCanvas.getElement()) {
                testParams.add(testParam.getTestParam());
            }

            testsParam.setTestParam(testParams);
            testsParam.setCommonSettings(testsParamCanvas.getCommonSettings());
        } else {
            testsParam = gson.fromJson(testsParamString, TestsParam.class);
        }

        Double percentProfile = testsParam.getCommonSettings().getBuildSettings().getPercentProfile();
        List<TestParam> testParams = testsParam.getTestParam();

        for (TestParam testParam : testParams) {
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

        testsParam.getCommonSettings().getBuildSettings().setPercentProfile(100.0);
        try (FileWriter writer = new FileWriter("./" + modifiedProfileName + ".json", StandardCharsets.UTF_8)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(testsParam));
            log.info("File Saved Successfully (./{}.json", modifiedProfileName);
        } catch (IOException e) {
            log.error("Error Save Profile: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        log.info("New Profile:");
        log.info(gson.toJson(testsParam));
    }
}
