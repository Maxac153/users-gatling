package gatling.users.authorization;

import gatling.common.helpers.PropertyHelper;
import gatling.users.authorization.scenario.AuthorizationScenario;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers;

public class AuthorizationUserTest extends Simulation {
    public AuthorizationUserTest() {
        Map<String, Object> property = PropertyHelper.readProperties(
                "common/common_properties.json",
                "common/redis_properties.json"
        );

        HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getOpenProfile(
                "tests/users/authorization/authorization_user_profile.json"
        );

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl(property.get("PROTOCOL") + "://" + property.get("HOST"))
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");

        if (Boolean.parseBoolean(property.get("DEBUG_ENABLE").toString()))
            this.setUp(
                    AuthorizationScenario.authorizationUserScenario(property)
                            .injectOpen(atOnceUsers(1))
            ).protocols(httpProtocol);
        else
            this.setUp(
                    AuthorizationScenario.authorizationUserScenario(property)
                            .injectOpen(profile.get("AUTHORIZATION_USER_SCENARIO"))
            ).protocols(httpProtocol);
    }
}
