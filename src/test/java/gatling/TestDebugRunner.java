package gatling;

import com.google.gson.Gson;
import gatling.__common.helpers.RedisHelper;
import helpers.DataFormatHelper;
import helpers.ReadFileHelper;
import lombok.extern.slf4j.Slf4j;
import redis.RedisDeleteKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TestDebugRunner {
    private static final Gson gson = new Gson();
    private static final HashMap<String, Object> redisProperty = ReadFileHelper.readEnv("redis");
    private static final AtomicInteger index = new AtomicInteger(0);
    private static final AtomicBoolean flagError = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {
        String javaPath = System.getProperty("JAVA_PATH");
        String jarPath = System.getProperty("JAR_PATH");
        String moduleName = System.getProperty("MODULE_NAME", "DEBUG");
        String dateNow = System.getProperty("DATE_NOW", "1970-01-01");
        String dateTimeNow = System.getProperty("DATE_TIME_NOW", "1970-01-01_00-00-00");
        ArrayList<String> testCase = ReadFileHelper.readTxt(System.getProperty("TEST_CASE_PATH"));
        HashMap<String, Object> classSimulations = ReadFileHelper.readSimulationCase(System.getProperty("CLASS_SIMULATIONS_PATH"));
        final int NUMBER_THREADS = Math.min(Integer.parseInt(System.getProperty("NUMBER_THREADS")), Runtime.getRuntime().availableProcessors());

        if (javaPath != null && jarPath != null) {
            SimpleDateFormat sdfDateTime = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
            sdfDateTime.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            Calendar calendarStart = Calendar.getInstance();
            ExecutorService executor = Executors.newFixedThreadPool(NUMBER_THREADS);

            for (int i = 0; i < NUMBER_THREADS; i++) {
                executor.submit(() -> runTestCase(
                        testCase,
                        classSimulations,
                        javaPath,
                        jarPath,
                        moduleName,
                        dateNow,
                        dateTimeNow
                ));
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    log.error("Time Waiting For Tasks To Finish.");
                    executor.shutdown();
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                executor.shutdown();
                Thread.currentThread().interrupt();
            }

            RedisDeleteKeys.main(new String[]{".*test"});
            RedisHelper.closeInstance();

            Calendar calendarEnd = Calendar.getInstance();
            String lineSeparator = "+--------------------------------------------+";
            log.info(lineSeparator);
            log.info(String.format("| Test Start Time:   %-23.23s |", sdfDateTime.format(calendarStart.getTime())));
            log.info(lineSeparator);
            log.info(String.format("| Test End Time:     %-23.23s |", sdfDateTime.format(calendarEnd.getTime())));
            log.info(lineSeparator);
            log.info(String.format("| Test Duration:     %-23.23s |", DataFormatHelper.format((calendarEnd.getTimeInMillis() - calendarStart.getTimeInMillis()) / 1000)));
            log.info(lineSeparator);

            if (flagError.get()) {
                throw new Exception("Test Debug Failed!");
            }
        } else {
            log.error("Incorrect Number Of Variables <Java Path> <Jar Path> <Path Test Cases> <Path Class Simulation> <Number Threads>");
        }
    }

    private static void runTestCase(
            ArrayList<String> testsList,
            HashMap<String, Object> classSimulations,
            String javaPath,
            String jarPath,
            String moduleName,
            String dateNow,
            String dateTimeNow
    ) {
        int currentIndex;
        while ((currentIndex = index.getAndIncrement()) < testsList.size()) {
            boolean exists = false;
            String testName = testsList.get(currentIndex);
            String[] tests = testName.split("_");

            for (int i = 0; i < tests.length; i++) {
                HashMap<String, String> testSettings = new HashMap<>();

                String redisKey = testName + "_test";
                if (i != 0) {
                    testSettings.put("REDIS_KEY_READ", redisKey);
                }

                testSettings.put("REDIS_KEY_ADD", redisKey);
                testSettings.put("ENV_PATHS", "redis");
                testSettings.put("DEBUG_ENABLE", "true");

                try {
                    Process process = Runtime.getRuntime()
                            .exec(javaPath
                                  + " -DMODULE_NAME=" + moduleName
                                  + " -DATE_NOW=" + dateNow
                                  + " -DDATE_TIME_NOW=" + dateTimeNow
                                  + " -DLOG_FILE_NAME=test_case-" + testName
                                  + " -DTEST_SETTINGS=\"" + gson.toJson(testSettings)
                                  + "\" -DCLASS_SIMULATION=" + classSimulations.get(tests[i]).toString()
                                  + " -cp " + jarPath + " io.gatling.app.Gatling -s ru.vtb.gatling.TestDebugRun"
                            );
                    String line;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }

                    process.destroy();
                } catch (Exception e) {
                    log.error(e.getMessage());
                    throw new RuntimeException(e);
                }

                if (!tests[i].contains("k")) {
                    exists = RedisHelper.getInstance(
                            redisProperty.get("REDIS_HOST").toString(),
                            Integer.valueOf(redisProperty.get("REDIS_PORT").toString()),
                            redisProperty.get("REDIS_LOGIN").toString(),
                            redisProperty.get("REDIS_PASSWORD").toString()
                    ).exists(redisKey);

                    if (!exists) {
                        log.error("Status: ERROR, Test Case: " + testName + " , Test: " + tests[i]);
                        flagError.set(true);
                        break;
                    }
                } else {
                    exists = true;
                }
            }
            if (exists) {
                log.info("Status: SUCCESSFUL, Test Case: " + testName);
            }
        }
    }
}
