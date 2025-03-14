package gatling.common.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gatling.common.models.profile.Profile;
import gatling.common.models.profile.Step;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;

public class PropertyHelper {
    private static final Gson gson = new Gson();

    private static String readProperty(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream inputStream = PropertyHelper.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null)
                throw new IOException("Resource not found: " + filePath);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null)
                contentBuilder.append(line).append(System.lineSeparator());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static Map<String, Object> readProperties(String... propertiesPaths) {
        HashMap<String, Object> properties = new HashMap<>();

        // Параметры из файлов
        Type type = new TypeToken<HashMap<String, String>>(){}.getType();
        for (String propertiesPath : propertiesPaths)
            properties.putAll(gson.fromJson(PropertyHelper.readProperty(propertiesPath), type));

        // Общие параметры из системы
        String commonSettings = System.getProperty("COMMON_SETTINGS");
        if (commonSettings != null)
            properties.putAll(gson.fromJson(commonSettings.replace("\"", ""), type));

        // Параметры теста из системы
        String testSettings = System.getProperty("TEST_SETTINGS");
        if (testSettings != null)
            properties.putAll(gson.fromJson(testSettings.replace("\"", ""), type));

        return properties;
    }

    public static HashMap<String, OpenInjectionStep[]> getOpenProfile(String profilePath) {
        String systemProfile = System.getProperty("TEST_PROFILE");

        Type listType = new TypeToken<ArrayList<Profile>>(){}.getType();
        ArrayList<Profile> profiles = gson.fromJson(
                (systemProfile == null) ? PropertyHelper.readProperty(profilePath) : systemProfile.replace("\"", ""),
                listType
        );

        // Создание Map<threadGroupName, profile> профилей для каждой нагрузочной катушки в тесте
        HashMap<String, OpenInjectionStep[]> profileMap = new HashMap<>();
        for (Profile profile : profiles) {
            ArrayList<OpenInjectionStep> loadProfile = new ArrayList<>();
            for (Step step : profile.getSteps()) {
                // RAMP_UP
                loadProfile.add(rampUsersPerSec(step.getStartTps()).to(step.getEndTps()).during((long) (step.getRampTime() * 60)));
                // HOLD
                loadProfile.add(constantUsersPerSec(step.getEndTps()).during((long) (step.getHoldTime() * 60)));
            }
            profileMap.put(profile.getScenarioName(), loadProfile.toArray(new OpenInjectionStep[0]));
        }

        return profileMap;
    }

    private static int calculateUsers(double rps, double throughput) {
        double pacing = (1 / throughput) * 60;
        double requiredIntensityPerMinute = rps * 60;
        return (int) Math.ceil(requiredIntensityPerMinute / (60 / pacing));
    }

    private static double calculateThroughputPerMinute(double rps, long scriptExecutionTime) {
        double requiredIntensityPerMinute = rps * 60;
        int threads = (int) Math.ceil(requiredIntensityPerMinute / ((double) 60 / scriptExecutionTime));
        double executionTime = threads / rps;
        double scale = Math.pow(10, 3);
        return Math.ceil(60 / executionTime * scale) / scale;
    }

    public static HashMap<String, ClosedInjectionStep[]> getClosedProfile(String profilePath, Map<String, Object> property) {
        String systemProfile = System.getProperty("TEST_PROFILE");

        Type listType = new TypeToken<ArrayList<Profile>>(){}.getType();
        ArrayList<Profile> profiles = gson.fromJson(
                (systemProfile == null) ? readProperty(profilePath) : systemProfile.replace("\"", ""),
                listType
        );

        HashMap<String, ClosedInjectionStep[]> profileMap = new HashMap<>();
        for (Profile profile : profiles) {
            ArrayList<ClosedInjectionStep> loadProfile = new ArrayList<>();
            double throughputPerMinute = calculateThroughputPerMinute(profile.getSteps().get(0).getEndTps(), profile.getScriptExecutionTime());
            property.put("SCRIPT_EXECUTION_TIME", profile.getScriptExecutionTime());

            for (Step step : profile.getSteps()) {
                int rampUpUsers = calculateUsers(step.getStartTps(), throughputPerMinute);
                int holdUsers = calculateUsers(step.getEndTps(), throughputPerMinute);

                // RAMP_UP
                loadProfile.add(rampConcurrentUsers(rampUpUsers).to(holdUsers).during((long) (step.getRampTime() * 60)));

                // HOLD
                loadProfile.add(constantConcurrentUsers(holdUsers).during((long) (step.getHoldTime() * 60)));
            }
            profileMap.put(profile.getScenarioName(), loadProfile.toArray(new ClosedInjectionStep[0]));
        }

        return profileMap;
    }
}
