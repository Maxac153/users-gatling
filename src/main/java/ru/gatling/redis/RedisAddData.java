package ru.gatling.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import ru.gatling.helpers.ReadFileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RedisAddData {
    public static void main(String[] args) {
        String key = System.getProperty("KEY");
        String jsonDataPath = System.getProperty("JSON_DATA_PATH");
        String addMode = System.getProperty("ADD_MODE");

        if (key != null && jsonDataPath != null && addMode != null) {
            HashMap<String, Object> properties = new HashMap<>(ReadFileHelper.readEnv("redis"));
            String redisHost = System.getProperty("REDIS_HOST", properties.get("REDIS_HOST").toString());
            int redisPort = Integer.parseInt(System.getProperty("REDIS_PORT", properties.get("REDIS_PORT").toString()));

            Jedis jedis = new Jedis(redisHost, redisPort);
            String data;

            try (Stream<String> lines = Files.lines(Paths.get(jsonDataPath))) {
                data = lines.collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            switch (addMode) {
                case "FIRST" -> {
                    jedis.lpush(key, data);
                    log.info("Successful Data Addition");
                }
                case "LAST" -> {
                    jedis.rpush(key, data);
                    log.info("Successful Data Addition");
                }
                default -> log.error("Incorrect Add Mode Expected: <FIRST or LAST>");
            }

            jedis.close();
        } else {
            log.error("Incorrect Number Of Variables <Key> <JSON Data Path> <addMode (FIRST, LAST)>");
        }
    }
}
