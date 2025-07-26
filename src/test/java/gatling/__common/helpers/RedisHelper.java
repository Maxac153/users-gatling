package gatling.__common.helpers;

import models.redis.RedisAddMode;
import models.redis.RedisReadMode;
import redis.clients.jedis.Jedis;

public class RedisHelper {
    private static RedisHelper instance;
    private final Jedis jedis;

    private RedisHelper(String redisHost, Integer redisPort) {
        jedis = new Jedis(redisHost, redisPort);
    }

    private RedisHelper(String redisHost, Integer redisPort, String username, String password) {
        jedis = new Jedis(redisHost, redisPort);
        jedis.auth(username, password);
    }

    public static synchronized RedisHelper getInstance() {
        return instance;
    }

    public static synchronized RedisHelper getInstance(String redisHost, Integer redisPort) {
        if (instance == null) {
            instance = new RedisHelper(redisHost, redisPort);
        }

        return instance;
    }

    public static synchronized RedisHelper getInstance(String redisHost, Integer redisPort, String username, String password) {
        if (instance == null) {
            instance = new RedisHelper(redisHost, redisPort, username, password);
        }

        return instance;
    }

    public synchronized void add(String key, String data, RedisAddMode addMode) {
        switch (addMode) {
            case FIRST -> jedis.lpush(key, data);
            case LAST -> jedis.rpush(key, data);
        }
    }

    public synchronized void read(String key, RedisReadMode readMode, Boolean keep) {
        String result;

        if (readMode == RedisReadMode.FIRST) {
            result = jedis.lpop(key);
            if (result == null) {
                return;
            }
            if (keep) {
                jedis.rpush(key, result);
            }
        } else if (readMode == RedisReadMode.LAST) {
            result = jedis.rpop(key);
            if (result == null) {
                return;
            }
            if (keep) {
                jedis.lpush(key, result);
            }
        } else {
        }

    }

    public synchronized boolean exists(String key) {
        return jedis.exists(key);
    }

    public synchronized void deleteKey(String key) {
        jedis.del(key);
    }

    private void close() {
        jedis.close();
        instance = null;
    }

    public static synchronized void closeInstance() {
        if (instance != null) {
            instance.close();
        }
    }
}
