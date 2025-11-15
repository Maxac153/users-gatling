package ru.gatling;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestDebugRun extends Simulation {
    private static final Gson GSON = new Gson();

    {
        setUp(testRunner());
    }

    private PopulationBuilder testRunner() throws RuntimeException {
        try {
            Type type = new TypeToken<HashMap<String, String>>() {
            }.getType();
            HashMap<String, String> testSettings = GSON.fromJson(System.getProperty("TEST_SETTINGS").replace("\"", ""), type);
            Class<?> classSimulation = Class.forName(System.getProperty("CLASS_SIMULATION"));
            Method method = classSimulation.getMethod("run", Map.class, Map.class, ArrayList.class);
            return (PopulationBuilder) method.invoke(
                    classSimulation.getDeclaredConstructor().newInstance(),
                    testSettings,
                    null,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
