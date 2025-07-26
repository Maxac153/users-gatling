package redis;

import com.google.gson.Gson;
import helpers.ReadFileHelper;
import lombok.extern.slf4j.Slf4j;
import models.profile.Canvas;
import models.profile.Elements;
import models.profile.TestParam;
import models.profile.TestsParam;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static helpers.PropertyHelper.getStepPace;

@Slf4j
public class RedisDataChecker {
    public static void main(String[] args) throws Exception {
        String profilePath = System.getProperty("PROFILE_PATH", "profiles/test_profile.json");
        TestsParam testsParam = new TestsParam();
        String testsParamString = ReadFileHelper.readProfile(profilePath);

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

        boolean flagError = false;
        HashMap<String, Object> properties = new HashMap<>(ReadFileHelper.readEnv("redis"));
        String redisHost = System.getProperty("REDIS_HOST", properties.get("REDIS_HOST").toString());
        int redisPort = Integer.parseInt(System.getProperty("REDIS_PORT", properties.get("REDIS_PORT").toString()));

        Jedis jedis = new Jedis(redisHost, redisPort);

        String lineSeparator = "+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+";
        log.info(lineSeparator);
        log.info("|                                            key                                   | Number Of Records Redis | Expected Number Of Records |    Difference    |      %     |");
        log.info(lineSeparator);

        for (String key : testData.keySet()) {
            long expectedNumberRecords = testData.get(key);
            long numberRecordsRedis = jedis.llen(key);
            long difference = numberRecordsRedis - expectedNumberRecords;
            double differencePercent = numberRecordsRedis / (double) expectedNumberRecords;

            String format = String.format("| %-80.80s | %-23.23s | %-26.26s | %-16.16s | %-10.10s |", key, numberRecordsRedis, expectedNumberRecords, difference, Math.round(differencePercent * 10000.0) / 100.0);

            if (difference < 0) {
                log.warn(format);
                if (differencePercent < 0.60) {
                    flagError = true;
                }
            } else {
                log.info(format);
            }

            log.info(lineSeparator);
        }
        jedis.close();

        if (flagError) {
            throw new Exception("Not Enough Data For The Test!");
        }
    }
}
