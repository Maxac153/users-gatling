package ru.gatling.users.authorization;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import ru.gatling.__common.helpers.Runnable;
import ru.gatling.helpers.PropertyHelper;
import ru.gatling.models.profile.Profile;
import ru.gatling.users.authorization.scenario.AuthorizationScenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthorizationAdminTest extends Simulation implements Runnable {
    @Override
    public PopulationBuilder run(
            String scenarioName,
            Map<String, Object> testSettings,
            ArrayList<Profile> testProfile
    ) {
        Map<String, Object> properties = PropertyHelper.readProperties(
                testSettings,
                "__common/common_properties.json"
        );

        HashMap<String, ClosedInjectionStep[]> profile = PropertyHelper.getClosedProfile(testProfile, properties);

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl("http://localhost:8080/login?from=%2F")
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");

        ScenarioBuilder scenarioBuilder = AuthorizationScenario.authorizationAdminScenario(
                scenarioName, properties, "AUTHORIZATION_ADMIN_SCENARIO"
        );

        if (!Boolean.parseBoolean(properties.get("DEBUG_ENABLE").toString())) {
            return scenarioBuilder.injectClosed(profile.get("AUTHORIZATION_ADMIN_SCENARIO")).protocols(httpProtocol);
        }
        return scenarioBuilder.injectOpen(CoreDsl.atOnceUsers(1));
    }
}
