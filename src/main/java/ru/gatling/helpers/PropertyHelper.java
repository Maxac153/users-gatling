package ru.gatling.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.Session;
import ru.gatling.models.profile.Profile;
import ru.gatling.models.profile.Step;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static io.gatling.javaapi.core.CoreDsl.*;

public class PropertyHelper {
    private static final Gson gson = new Gson();

    public static Map<String, Object> readProperties(
            Map<String, Object> testSettings,
            String... propertiesPaths
    ) {
        HashMap<String, Object> properties = new HashMap<>();

        // Параметры из файлов
        Type type = new TypeToken<HashMap<String, Object>>() {
        }.getType();
        for (String propertiesPath : propertiesPaths) {
            properties.putAll(gson.fromJson(ReadFileHelper.read(propertiesPath), type));
        }

        // Параметры теста из системы
        if (testSettings != null) {
            properties.putAll(testSettings);
        }

        // Параметры из .env (redis_login, redis_password, db_login, db_password, ...)
        properties.putAll(ReadFileHelper.readEnv(properties.get("ENV").toString()));

        return properties;
    }

    // Открытая модель нагрузки
    public static HashMap<String, OpenInjectionStep[]> getOpenProfile(ArrayList<Profile> profiles) {
        HashMap<String, OpenInjectionStep[]> profileMap = new HashMap<>();
        for (Profile profile : profiles) {
            ArrayList<OpenInjectionStep> loadProfile = new ArrayList<>();
            List<Step> steps = profile.getSteps();

            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                double startTps = (i == 0) ? 0.0 : steps.get(i - 1).getTps();

                loadProfile.add(rampUsersPerSec(startTps).to(step.getTps()).during((long) (step.getRampTime() * 60)));
                loadProfile.add(constantUsersPerSec(step.getTps()).during((long) (step.getHoldTime() * 60)));
            }

            profileMap.put(profile.getScenarioName(), loadProfile.toArray(new OpenInjectionStep[0]));
        }

        return profileMap;
    }

    public static Function<Session, Duration> getStepPause(String scenarioName) {
        return session -> {
            if (session.getBoolean("DEBUG_ENABLE")) {
                return Duration.ofMillis(0);
            }

            long pause;
            long transactionEndTime = System.currentTimeMillis();
            TreeMap<Long, Long> stepsPace = session.get("PACING_" + scenarioName);

            for (Long key : stepsPace.keySet()) {
                if (transactionEndTime < key) {
                    pause = stepsPace.get(key) - (transactionEndTime - session.getLong("transaction_start_time"));
                    if (pause < 0) {
                        return Duration.ofMillis(0);
                    } else {
                        return Duration.ofMillis(pause);
                    }
                }
            }

            pause = stepsPace.get(stepsPace.lastKey()) - (transactionEndTime - session.getLong("transaction_start_time"));

            if (pause < 0) {
                return Duration.ofMillis(0);
            } else {
                return Duration.ofMillis(pause);
            }
        };
    }

    // Расчёт pace для каждого шага
    public static long getStepPace(double tps, double pacing) {
        double pace = 1 / tps;

        if (tps > 0 && pace > pacing) {
            return (long) Math.floor(pace * 1000);
        } else if (tps > 0 && 1 - pace / pacing < 0.96) {
            return (long) Math.floor(pace * Math.ceil(pacing / pace) * 3 * 1000);
        }

        return (long) Math.floor(pacing * 1000);
    }

    public static HashMap<String, ClosedInjectionStep[]> getClosedProfile(
            ArrayList<Profile> profiles,
            Map<String, Object> properties
    ) {
        long currentTime = System.currentTimeMillis();

        HashMap<String, ClosedInjectionStep[]> profileMap = new HashMap<>();
        for (Profile profile : profiles) {
            ArrayList<ClosedInjectionStep> loadProfile = new ArrayList<>();
            TreeMap<Long, Long> stepsPace = new TreeMap<>();
            long pacing = profile.getPacing();

            List<Step> steps = profile.getSteps();
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                double startTps = (i == 0) ? 0.0 : steps.get(i - 1).getTps();
                currentTime += (long) ((step.getRampTime() * 60 + step.getHoldTime() * 60) * 1000);

                long pacingRumpUp = getStepPace(startTps, pacing);
                long pacingHold = getStepPace(step.getTps(), pacing);
                stepsPace.put(currentTime, pacingHold);

                int rampUpUsers = (int) Math.round(startTps * pacingRumpUp / 1000);
                int holdUsers = (int) Math.round(step.getTps() * pacingHold / 1000);

                loadProfile.add(rampConcurrentUsers(rampUpUsers).to(holdUsers).during((long) (step.getRampTime() * 60)));
                loadProfile.add(constantConcurrentUsers(holdUsers).during((long) (step.getHoldTime() * 60)));
            }

            properties.put("PACING_" + profile.getScenarioName(), stepsPace);
            profileMap.put(profile.getScenarioName(), loadProfile.toArray(new ClosedInjectionStep[0]));
        }

        return profileMap;
    }
}
