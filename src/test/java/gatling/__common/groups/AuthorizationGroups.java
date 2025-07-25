package gatling.__common.groups;

import gatling.__common.helpers.RedisClientHelper;
import gatling.__common.helpers.RedisHelper;
import gatling.__common.steps.HttpCommonSteps;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpDsl;
import models.redis.RedisAddMode;
import models.redis.RedisReadMode;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;

public class AuthorizationGroups {
    public static ChainBuilder authorizationUser = group("uc_users_authentication_user").on(
            exec(session -> session.set("redis_read_mode", RedisReadMode.FIRST))
                    .exec(RedisClientHelper.readList)
                    .exec(HttpCommonSteps.mainPage)
                    .exec(HttpCommonSteps.authorization)
    );

    public static ChainBuilder authorizationAdmin = group("uc_users_authentication_admin_#{REDIS_KEY_ADD}").on(
            exec(session -> session.set("login", "manager@mail.ru").set("password", "1"))
                    .exec(session -> {
                        RedisHelper.getInstance("localhost", 6379)
                                .read(
                                        session.getString("REDIS_KEY_READ"),
                                        RedisReadMode.FIRST,
                                        false
                                );

                        return session;
                    })
                    .exec(HttpDsl.http("ur_users_open_main_page_#{REDIS_KEY_ADD}").get("/"))
                    .exec(session -> {
                        RedisHelper.getInstance("localhost", 6379)
                                .add(
                                        session.getString("REDIS_KEY_ADD"),
                                        "test_data",
                                        RedisAddMode.LAST
                                );

                        return session;
                    })
            // .exec(HttpDsl.http("ur_users_open_main_page_2").get("/"))
    );
}
