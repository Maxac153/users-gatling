package ru.gatling.__common.helpers;

import io.gatling.javaapi.core.ChainBuilder;
import ru.gatling.models.redis.RedisAddMode;

import static io.gatling.javaapi.core.CoreDsl.exec;


public class SaveRedisDataAfterRequestHelper {
    public static ChainBuilder get = exec(session -> {
        RedisHelper.getInstance(session.getString("REDIS_HOST"), session.getInt("REDIS_PORT"), session.getString("REDIS_LOGIN"), session.getString("REDIS_PASSWORD"))
                .add(
                        session.getString("REDIS_KEY_ADD"),
                        session.getString("redis_data_add"),
                        RedisAddMode.LAST
                );

        return session;
    });
}
