package gatling.users.loading_avatar.groups;

import gatling.common.steps.CommonSteps;
import gatling.users.loading_avatar.steps.LoadingAvatarSteps;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;

public class LoadingAvatarGroups {
    public static ChainBuilder loadingAvatar = group("uc_users_loading_avatar").on(
            exec(session -> session.set("email", "manager@mail.ru")),
            exec(LoadingAvatarSteps.loadingAvatar),
            exec(CommonSteps.logout),
            exec(CommonSteps.mainPage)
    );
}
