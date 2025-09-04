package ru.gatling.users.registration.scenario;

import ru.gatling.users.registration.gropus.RegistrationGroups;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

public class RegistrationScenario {
    public static ScenarioBuilder registration(Map<String, Object> property) {
        return CoreDsl.scenario("users_registration_scenario")
                .exec(session -> session.setAll(property))
                .exec(RegistrationGroups.registration);
    }
}
