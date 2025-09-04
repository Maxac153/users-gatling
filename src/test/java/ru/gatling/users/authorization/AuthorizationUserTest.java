package ru.gatling.users.authorization;

import ru.gatling.__common.helpers.Runnable;
import ru.gatling.users.authorization.scenario.AuthorizationScenario;
import ru.gatling.helpers.PropertyHelper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import ru.gatling.models.profile.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthorizationUserTest extends Simulation implements Runnable {
    @Override
    public PopulationBuilder run(
            Map<String, Object> testSettings,
            ArrayList<Profile> testProfile
    ) {
        Map<String, Object> properties = PropertyHelper.readProperties(
                testSettings,
                "__common/common_properties.json",
                "__common/redis_properties.json"
        );

        HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getOpenProfile(testProfile);

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl(properties.get("PROTOCOL") + "://" + properties.get("HOST"))
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");

        ScenarioBuilder scenarioBuilder = AuthorizationScenario.authorizationUserScenario(properties);

        if (!Boolean.parseBoolean(properties.get("DEBUG_ENABLE").toString())) {
            return scenarioBuilder.injectOpen(profile.get("REGISTRATION_SCENARIO")).protocols(httpProtocol);
        }
        return scenarioBuilder.injectOpen(CoreDsl.atOnceUsers(1));
    }
}
