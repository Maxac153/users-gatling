package gatling.users.loading_avatar.gropus;

import gatling.common.steps.StepsCommon;
import gatling.users.loading_avatar.steps.Steps;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;

public class LoadingAvatarGroups {
    public static ChainBuilder loadingAvatar = group("uc_users_loading_avatar").on(
            exec(session -> session.set("email", "manager@mail.ru")),
            exec(Steps.loadingAvatar),
            exec(StepsCommon.logout),
            exec(StepsCommon.mainPage)
    );
}
