package redis;

import helpers.DataFormatHelper;
import helpers.ReadFileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.HashMap;

public class RedisInfo {
    private static final Logger log = LoggerFactory.getLogger(RedisInfo.class);
    private static final long[] units = {1_099_511_627_766L, 1_073_741_824, 1_048_576, 1_024};
    private static final String[] unitNames = {"Tb", "Gb", "Mb", "Kb"};

    private static String formatSize(long sizeInBytes) {
        for (int i = 0; i < units.length; i++) {
            if (Math.abs(sizeInBytes) >= units[i]) {
                return String.format("%.2f %s", (double) sizeInBytes / units[i], unitNames[i]);
            }
        }

        return String.format("%d B", sizeInBytes);
    }

    public static void main(String[] args) {
        HashMap<String, Object> properties = new HashMap<>(ReadFileHelper.readEnv("redis"));
        String redisHost = System.getProperty("REDIS_HOST", properties.get("REDIS_HOST").toString());
        int redisPort = Integer.parseInt(System.getProperty("REDIS_PORT", properties.get("REDIS_PORT").toString()));

        Jedis jedis = new Jedis(redisHost, redisPort);

        String info = jedis.info();
        String[] lines = info.split("\r\n");
        String connectionClients = null;

        for (String line : lines) {
            if (line.startsWith("connected_clients")) {
                connectionClients = line.split(":")[1].trim();
                break;
            }
        }

        String infoMemory = jedis.info("memory");
        String[] memoryLines = infoMemory.split("\n");
        String totalMemory = null;
        String usedMemory = null;

        for (String line : memoryLines) {
            if (line.startsWith("used_memory")) {
                usedMemory = line.split(":")[1].trim();
            } else if (line.startsWith("total_system_memory")) {
                totalMemory = line.split(":")[1].trim();
            }
        }

        log.info("REDIS SERVER INFORMATION");
        log.info("Host: {}", redisHost);
        log.info("Port: {}", redisPort);
        log.info("Connected Clients: {}", connectionClients);
        log.info("Total Memory: {}", formatSize(Long.parseLong(totalMemory != null ? totalMemory : "-")));
        log.info("Memory Used: {}", formatSize(Long.parseLong(usedMemory != null ? usedMemory : "-")));

        // Исправить на скан
        for (String key : jedis.keys("*")) {
            String type = jedis.type(key);
            String count = switch (type) {
                case "list" -> String.valueOf(jedis.llen(key));
                case "set" -> String.valueOf(jedis.scard(key));
                case "hash" -> String.valueOf(jedis.hlen(key));
                case "zset" -> String.valueOf(jedis.zcard(key));
                default -> "-";
            };

            long size = jedis.memoryUsage(key);
            String ttl;
            Long ttlValue = jedis.ttl(key);

            if (ttlValue == -1) {
                ttl = "-";
            } else {
                ttl = DataFormatHelper.format(ttlValue);
            }

            log.info(String.format("| %-80.80s | %-6.6s | %-21.21s | %-13.13s | %-27.27s |", key, type, count, formatSize(size), ttl));
        }
        log.info("+------+----+----+----+");
        jedis.close();
    }
}
