package gatling.users.loading_avatar.scenario;

import gatling.common.steps.StepsCommon;
import gatling.users.loading_avatar.steps.Steps;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

public class LoadingAvatarScenario {
    public static ScenarioBuilder loadingAvatarScenario(Map<String, Object> property) {
        return CoreDsl.scenario("uc_users_loading_avatar")
                .exec(session -> session.setAll(property))
                .exec(StepsCommon.mainPage)
                .exec(session -> session.set("login", "manager@mail.ru").set("password", "1"))
                .exec(StepsCommon.authorizationAdmin)
                .exec(session -> session.set("email", "manager@mail.ru"))
                .exec(Steps.loadingAvatar)
                .exec(StepsCommon.logout)
                .exec(StepsCommon.mainPage);
    }
}
