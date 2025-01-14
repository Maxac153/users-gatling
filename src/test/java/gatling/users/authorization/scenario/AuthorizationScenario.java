package gatling.users.authorization.scenario;

import gatling.common.groups.AuthorizationGroups;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

public class AuthorizationScenario {
    public static ScenarioBuilder authorizationUserScenario(Map<String, Object> property) {
        return CoreDsl.scenario("users_authentication_user_scenario")
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationUserGroup);
    }

    public static ScenarioBuilder authorizationAdminScenario(Map<String, Object> property) {
        return CoreDsl.scenario("users_authentication_admin_scenario")
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationAdminGroup);
    }
}
