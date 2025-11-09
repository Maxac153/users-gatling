package ru.gatling.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.gatling.models.profile.Canvas;
import ru.gatling.models.profile.TestParam;
import ru.gatling.models.profile.TestsParam;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class ReadFileHelper {
    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<HashMap<String, String>>() {
    }.getType();

    public static String read(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (InputStream inputStream = ReadFileHelper.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new IOException("Resource Not Found: " + filePath);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;

            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return contentBuilder.toString();
    }

    public static ArrayList<String> readTxt(String filePath) {
        ArrayList<String> strings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                strings.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return strings;
    }

    private static String readJson(String filePath) {
        StringBuilder data = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }

        return data.toString();
    }

    public static HashMap<String, Object> readSimulationCase(String testCasePath) {
        return new HashMap<>(gson.fromJson(ReadFileHelper.readJson(testCasePath), type));
    }

    public static HashMap<String, Object> readEnv(String env) {
        HashMap<String, Object> envMap = new HashMap<>();
        if (!env.isEmpty()) {
            String[] envPaths = env.split(",");

            for (String envPath : envPaths) {
                if (!envPath.isEmpty()) {
                    envMap.putAll(gson.fromJson(ReadFileHelper.readJson("./env/" + envPath + ".json"), type));
                }
            }
        }

        return envMap;
    }

    public static TestsParam readProfile(String profilePath) {
        String json = ReadFileHelper.readJson(profilePath);
        TestsParam testsParam = new TestsParam();

        if (profilePath.contains("canvas")) {
            Canvas testsParamCanvas = new Gson().fromJson(json, Canvas.class);
            HashMap<String, TestParam> testParams = new HashMap<>(testsParamCanvas.getElement());

            testsParam.setTestParam(testParams);
            testsParam.setCommonSettings(testsParamCanvas.getCommonSettings());
        } else {
            testsParam = new Gson().fromJson(json, TestsParam.class);
        }

        return testsParam;
    }

}
