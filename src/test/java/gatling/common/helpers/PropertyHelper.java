package gatling.common.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gatling.common.models.profile.Profile;
import gatling.common.models.profile.Step;
import gatling.common.models.profile.TestParam;
import io.gatling.javaapi.core.OpenInjectionStep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;

public class PropertyHelper {
    private static final Gson gson = new Gson();

    public static String readProperty(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream inputStream = PropertyHelper.class.getClassLoader().getResourceAsStream(filePath)) {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static Map<String, Object> readProperties(
            String... propertiesPaths
    ) {
        HashMap<String, Object> properties = new HashMap<>();

        // Параметры и профиль из файлов
        for (String propertiesPath : propertiesPaths) {
            TestParam testParam = gson.fromJson(PropertyHelper.readProperty(propertiesPath), TestParam.class);
            if (testParam.getProperties() != null)
                properties.putAll(testParam.getProperties());
        }

        // Общие параметры из системы
        String commonSettings = System.getProperty("COMMON_SETTINGS");
        if (commonSettings != null) {
            TestParam systemCommonProperties = gson.fromJson(
                    commonSettings,
                    TestParam.class
            );
            properties.putAll(systemCommonProperties.getProperties());
        }

        // Параметры теста из системы
        String testSettings = System.getProperty("TEST_SETTINGS");
        if (testSettings != null) {
            TestParam systemTestProperties = gson.fromJson(
                    testSettings,
                    TestParam.class
            );
            properties.putAll(systemTestProperties.getProperties());
        }

        return properties;
    }

    public static HashMap<String, OpenInjectionStep[]> getProfile(String profilePath) {
        String systemProfile = System.getProperty("TEST_PROFILE");
        Type listType = new TypeToken<ArrayList<Profile>>(){}.getType();
        ArrayList<Profile> profiles = gson.fromJson(
                Objects.requireNonNullElseGet(systemProfile, () -> PropertyHelper.readProperty(profilePath)),
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
}
