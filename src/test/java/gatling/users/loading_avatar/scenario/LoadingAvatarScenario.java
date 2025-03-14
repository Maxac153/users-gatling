package gatling.users.loading_avatar.scenario;

import gatling.common.groups.AuthorizationGroups;
import gatling.users.loading_avatar.groups.LoadingAvatarGroups;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

public class LoadingAvatarScenario {
    public static ScenarioBuilder loadingAvatarScenario(Map<String, Object> property) {
        return CoreDsl.scenario("users_loading_avatar_scenario")
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationAdmin)
                .exec(LoadingAvatarGroups.loadingAvatar);
    }
}
