package gatling.common.groups;

import gatling.common.helpers.RedisClientHelper;
import gatling.common.models.redis.RedisReadType;
import gatling.common.steps.StepsCommon;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;

public class AuthorizationGroups {
    public static ChainBuilder authorizationUserGroup = group("uc_users_authentication_user").on(
            exec(session -> session.set("redis_read_mode", RedisReadType.FIRST.getReadMode())),
            exec(RedisClientHelper.readList),
            exec(StepsCommon.mainPage),
            exec(StepsCommon.authorization)
    );

    public static ChainBuilder authorizationAdminGroup = group("uc_users_authentication_admin").on(
            exec(StepsCommon.mainPage),
            exec(session -> session.set("login", "manager@mail.ru").set("password", "1")),
            exec(StepsCommon.authorizationAdmin)
    );
}
