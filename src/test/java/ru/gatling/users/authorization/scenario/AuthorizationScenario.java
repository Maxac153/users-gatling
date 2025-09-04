package ru.gatling.users.authorization.scenario;

import ru.gatling.__common.groups.AuthorizationGroups;
import ru.gatling.helpers.PropertyHelper;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;
import java.util.UUID;

public class AuthorizationScenario {
    public static ScenarioBuilder authorizationUserScenario(Map<String, Object> property) {
        return CoreDsl.scenario("users_authentication_user_scenario")
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationUser);
    }

    public static ScenarioBuilder authorizationAdminScenario(Map<String, Object> property, String scenarioName) {
        return CoreDsl.scenario("users_authentication_admin_scenario_" + UUID.randomUUID())
                .exec(session -> session.set("transaction_start_time", System.currentTimeMillis()))
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationAdmin)
                .pause(PropertyHelper.getStepPause(scenarioName));
    }
}
