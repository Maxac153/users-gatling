package gatling.users.loading_avatar.scenario;

import gatling.common.steps.StepsCommon;
import gatling.users.loading_avatar.steps.Steps;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.exec;

public class LoadingAvatarScenario {
    public static ScenarioBuilder loadingAvatarScenario(Map<String, Object> property) {
        return CoreDsl.scenario("users_loading_avatar_scenario")
                .group("uc_users_loading_avatar").on(
                    exec(session -> session.setAll(property)),
                    exec(StepsCommon.mainPage),
                    exec(session -> session.set("login", "manager@mail.ru").set("password", "1")),
                    exec(StepsCommon.authorizationAdmin),
                    exec(session -> session.set("email", "manager@mail.ru")),
                    exec(Steps.loadingAvatar),
                    exec(StepsCommon.logout),
                    exec(StepsCommon.mainPage)
                );
    }
}
