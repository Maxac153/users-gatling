package ru.gatling.redis;

import ru.gatling.helpers.ReadFileHelper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.regex.Pattern;

@Slf4j
public class RedisDeleteKeys {
    public static void main(String[] args) {
        if (args.length > 0) {
            HashMap<String, Object> properties = new HashMap<>(ReadFileHelper.readEnv("redis"));
            String redisHost = System.getProperty("REDIS_HOST", properties.get("REDIS_HOST").toString());
            int redisPort = Integer.parseInt(System.getProperty("REDIS_PORT", properties.get("REDIS_PORT").toString()));

            Jedis jedis = new Jedis(redisHost, redisPort);

            log.info("Start Delete Keys:");
            for (String arg : args) {
                // Переписать на скан
                for (String redisKey : jedis.keys("*")) {
                    if (Pattern.matches(arg, redisKey)) {
                        jedis.del(redisKey);
                        log.info("Delete Key: " + redisKey);
                    }
                }
            }

            jedis.close();
            log.info("End Delete Keys.");
        } else {
            log.info("Specify The Pattern To Delete!");
        }
    }
}
