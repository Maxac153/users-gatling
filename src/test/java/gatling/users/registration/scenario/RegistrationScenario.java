package gatling.users.registration.scenario;

import gatling.common.helpers.RedisClientHelper;
import gatling.common.models.redis.RedisAddType;
import gatling.common.steps.StepsCommon;
import gatling.users.registration.helper.RegistrationUserRandomHelper;
import gatling.users.registration.steps.Steps;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.Map;

public class RegistrationScenario {
    public static ScenarioBuilder registration(Map<String, Object> property) {
        return CoreDsl.scenario("uc_users_registration")
                .exec(session -> session.setAll(property))
                .exec(StepsCommon.mainPage)
                .exec(session -> session.set("json", RegistrationUserRandomHelper.get()))
                .exec(Steps.registration)
                .exec(StepsCommon.logout)
                .exec(StepsCommon.mainPage)
                .exec(session -> session.set("redis_add_mode", RedisAddType.LAST.getAddMode()))
                .exec(RedisClientHelper.addList);
    }
}
