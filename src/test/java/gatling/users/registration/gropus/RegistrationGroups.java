package gatling.users.registration.gropus;

import gatling.common.helpers.RedisClientHelper;
import gatling.common.models.redis.RedisAddType;
import gatling.common.steps.StepsCommon;
import gatling.users.registration.helper.RegistrationUserRandomHelper;
import gatling.users.registration.steps.Steps;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.exec;

public class RegistrationGroups {
    public static ChainBuilder registration = group("uc_users_registration").on(
            exec(StepsCommon.mainPage),
            exec(session -> session.set("json", RegistrationUserRandomHelper.get())),
            exec(Steps.registration),
            exec(StepsCommon.logout),
            exec(StepsCommon.mainPage),
            exec(session -> session.set("redis_add_mode", RedisAddType.LAST.getAddMode())),
            exec(RedisClientHelper.addList)
    );
}
