package ru.gatling.__common.helpers;

import io.gatling.javaapi.core.PopulationBuilder;
import ru.gatling.models.profile.Profile;

import java.util.ArrayList;
import java.util.Map;

public interface Runnable {
    PopulationBuilder run(String scenarioName, Map<String, Object> testSettings, ArrayList<Profile> testProfile);
}
