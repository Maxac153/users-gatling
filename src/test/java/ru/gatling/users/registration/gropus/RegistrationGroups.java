package ru.gatling.users.registration.gropus;

import ru.gatling.__common.helpers.RedisClientHelper;
import ru.gatling.models.redis.RedisAddMode;
import ru.gatling.__common.steps.HttpCommonSteps;
import ru.gatling.users.registration.helper.RegistrationUserRandomHelper;
import ru.gatling.users.registration.steps.Steps;
import io.gatling.javaapi.core.ChainBuilder;

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
