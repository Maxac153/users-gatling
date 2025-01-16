package gatling.users.loading_avatar.scenario;

import gatling.common.groups.AuthorizationGroups;
import gatling.common.steps.StepsCommon;
import gatling.users.loading_avatar.gropus.LoadingAvatarGroups;
import gatling.users.loading_avatar.steps.Steps;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.exec;

public class LoadingAvatarScenario {
    public static ScenarioBuilder loadingAvatarScenario(Map<String, Object> property) {
        return CoreDsl.scenario("users_loading_avatar_scenario")
                .exec(session -> session.setAll(property))
                .exec(AuthorizationGroups.authorizationAdmin)
                .exec(LoadingAvatarGroups.loadingAvatar);
    }
}
