package gatling.users.registration;

import gatling.__common.helpers.Runnable;
import gatling.users.registration.scenario.RegistrationScenario;
import helpers.PropertyHelper;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import models.profile.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;

public class RegistrationTest extends Simulation implements Runnable {
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

        HashMap<String, ClosedInjectionStep[]> profile = PropertyHelper.getClosedProfile(testProfile, properties);

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl(properties.get("PROTOCOL") + "://" + properties.get("HOST"))
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");

        ScenarioBuilder scenarioBuilder = RegistrationScenario.registration(properties);

        if (!Boolean.parseBoolean(properties.get("DEBUG_ENABLE").toString())) {
            return scenarioBuilder.injectClosed(profile.get("REGISTRATION_SCENARIO")).protocols(httpProtocol);
        }
        return scenarioBuilder.injectOpen(atOnceUsers(1));
    }

}
