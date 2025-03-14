package gatling.users.registration;

import gatling.common.helpers.PropertyHelper;
import gatling.users.registration.scenario.RegistrationScenario;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers;

public class RegistrationTest extends Simulation {
    public RegistrationTest() {
        Map<String, Object> property = PropertyHelper.readProperties(
                "common/common_properties.json",
                "common/redis_properties.json"
        );

        HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getOpenProfile(
                "tests/users/registration/registration_profile.json"
        );

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl(property.get("PROTOCOL") + "://" + property.get("HOST"))
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");
        if (Boolean.parseBoolean(property.get("DEBUG_ENABLE").toString()))
            this.setUp(
                    RegistrationScenario.registration(property)
                            .injectOpen(atOnceUsers(1))
            ).protocols(httpProtocol);
        else
            this.setUp(
                    RegistrationScenario.registration(property)
                            .injectOpen(profile.get("REGISTRATION_SCENARIO"))
            ).protocols(httpProtocol);
    }
}
