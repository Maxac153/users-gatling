package ru.gatling.users.authorization.scenario;

import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;
import ru.gatling.__common.groups.AuthorizationGroups;
import ru.gatling.helpers.PropertyHelper;

import java.util.Map;

public class AuthorizationScenario {
    public static ScenarioBuilder authorizationUserScenario(String scenarioName, Map<String, Object> property, String profileScenarioName) {
        return CoreDsl.scenario(scenarioName)
                .exec(session -> session.set("transaction_start_time", System.currentTimeMillis()))
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationUser)
                .pause(PropertyHelper.getStepPause(profileScenarioName));
    }

    public static ScenarioBuilder authorizationAdminScenario(String scenarioName, Map<String, Object> property, String profileScenarioName) {
        return CoreDsl.scenario(scenarioName)
                .exec(session -> session.set("transaction_start_time", System.currentTimeMillis()))
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationAdmin)
                .pause(PropertyHelper.getStepPause(profileScenarioName));
    }
}
