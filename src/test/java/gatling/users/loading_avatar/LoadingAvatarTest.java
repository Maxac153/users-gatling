package gatling.users.loading_avatar;

import gatling.common.helpers.PropertyHelper;
import gatling.users.loading_avatar.scenario.LoadingAvatarScenario;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers;

public class LoadingAvatarTest extends Simulation {
    public LoadingAvatarTest() {
        Map<String, Object> property = PropertyHelper.readProperties(
                "common/common_propeLrties.json"
        );

        HashMap<String, OpenInjectionStep[]> profile = PropertyHelper.getProfile(
                "tests/users/loading_avatar/loading_avatar_profile.json"
        );

        HttpProtocolBuilder httpProtocol = HttpDsl.http
                .baseUrl(property.get("PROTOCOL") + "://" + property.get("HOST"))
                .disableCaching()
                .userAgentHeader("Gatling/Performance Test");

        if (Boolean.parseBoolean(property.get("DEBUG_ENABLE").toString()))
            this.setUp(
                    LoadingAvatarScenario.loadingAvatarScenario(property)
                            .injectOpen(atOnceUsers(1))
            ).protocols(httpProtocol);
        else
            this.setUp(
                    LoadingAvatarScenario.loadingAvatarScenario(property)
                            .injectOpen(profile.get("LOADING_AVATAR_SCENARIO"))
            ).protocols(httpProtocol);
    }
}
