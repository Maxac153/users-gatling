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

public class AuthorizationAdminTest extends Simulation {
    public AuthorizationAdminTest() {
        Map<String, Object> property = PropertyHelper.readProperties(
                "common/common_properties.json",
                "tests/users/authorization/authorization_admin_profile.json"
        );
        HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getProfile(property);

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl(property.get("PROTOCOL") + "://" + property.get("HOST"))
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");

        if (Boolean.parseBoolean(property.get("DEBUG_ENABLE").toString()))
            this.setUp(
                    AuthorizationScenario.authorizationAdminScenario(property)
                            .injectOpen(atOnceUsers(1))
            ).protocols(httpProtocol);
        else
            this.setUp(
                    AuthorizationScenario.authorizationAdminScenario(property)
                            .injectOpen(profile.get("AUTHORIZATION_ADMIN_SCENARIO"))
            ).protocols(httpProtocol);
    }
}
