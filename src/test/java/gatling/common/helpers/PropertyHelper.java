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
            String... pathsProperties
    ) {
        String profile = null;
        HashMap<String, Object> properties = new HashMap<>();

        // Параметры и профиль из файлов
        for (String pathProperties : pathsProperties) {
            TestParam testParam = gson.fromJson(PropertyHelper.readProperty(pathProperties), TestParam.class);
            if (testParam.getProperties() != null)
                properties.putAll(testParam.getProperties());

            if (testParam.getProfile() != null)
                profile = gson.toJson(testParam.getProfile());
        }

        // Общие параметры из системы
        TestParam systemCommonProperties = gson.fromJson(System.getProperty("COMMON_SETTINGS"), TestParam.class);
        if (systemCommonProperties != null)
            properties.putAll(systemCommonProperties.getProperties());

        // Параметры теста и параметры профиля из системы
        TestParam systemTestProperties = gson.fromJson(System.getProperty("TEST_SETTINGS"), TestParam.class);
        if (systemTestProperties != null) {
            properties.putAll(systemTestProperties.getProperties());
            profile = gson.toJson(systemTestProperties.getProfile());
        }

        properties.put("PROFILE", profile);
        return properties;
    }

    public static HashMap<String, OpenInjectionStep[]> getProfile(Map<String, Object> properties) {
        Type profileListType = new TypeToken<ArrayList<Profile>>() {
        }.getType();
        ArrayList<Profile> profiles = gson.fromJson((String) properties.get("PROFILE"), profileListType);

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
