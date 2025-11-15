package ru.gatling.users.t2_registration.gropus;

import io.gatling.javaapi.core.ChainBuilder;
import ru.gatling.__common.helpers.RedisClientHelper;
import ru.gatling.__common.steps.HttpCommonSteps;
import ru.gatling.models.redis.RedisAddMode;
import ru.gatling.users.t2_registration.helper.RegistrationUserRandomHelper;
import ru.gatling.users.t2_registration.steps.Steps;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;

public class RegistrationGroups {
    public static ChainBuilder registration = group("uc_registration").on(
            exec(HttpCommonSteps.mainPage)
                    .exec(session -> session.set("json", RegistrationUserRandomHelper.get()))
                    .exec(Steps.registration)
                    .exec(HttpCommonSteps.logout)
                    .exec(HttpCommonSteps.mainPage)
                    .exec(session -> session.set("redis_add_mode", RedisAddMode.LAST))
                    .exec(RedisClientHelper.addList)
    );
}
