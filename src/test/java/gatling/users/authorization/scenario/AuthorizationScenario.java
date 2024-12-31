package gatling.users.authorization.scenario;

import gatling.common.helpers.RedisClientHelper;
import gatling.common.models.redis.RedisReadType;
import gatling.common.steps.StepsCommon;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

public class AuthorizationScenario {
    public static ScenarioBuilder authorizationUserScenario(Map<String, Object> property) {
        return CoreDsl.scenario("uc_users_authentication_user")
                .exec(session -> session.setAll(property))
                .exec(session -> session.set("redis_read_mode", RedisReadType.FIRST.getReadMode()))
                .exec(RedisClientHelper.readList)
                .exec(StepsCommon.mainPage)
                .exec(StepsCommon.authorization)
                .exec(StepsCommon.logout)
                .exec(StepsCommon.mainPage);
    }

    public static ScenarioBuilder authorizationAdminScenario(Map<String, Object> property) {
        return CoreDsl.scenario("uc_users_authentication_admin")
                .exec(session -> session.setAll(property))
                .exec(StepsCommon.mainPage)
                .exec(session -> session.set("login", "manager@mail.ru").set("password", "1"))
                .exec(StepsCommon.authorizationAdmin)
                .exec(StepsCommon.logout)
                .exec(StepsCommon.mainPage);
    }
}
