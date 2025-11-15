package ru.gatling.users.t2_registration.scenario;

import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;
import ru.gatling.helpers.PropertyHelper;
import ru.gatling.users.t2_registration.gropus.RegistrationGroups;

import java.util.Map;

public class RegistrationScenario {
    public static ScenarioBuilder registration(String scenarioName, Map<String, Object> property, String profileScenarioName) {
        return CoreDsl.scenario(scenarioName)
                .exec(session -> session.set("transaction_start_time", System.currentTimeMillis()))
                .exec(session -> session.setAll(property))
                .exec(RegistrationGroups.registration)
                .pause(PropertyHelper.getStepPause(profileScenarioName));
    }
}
