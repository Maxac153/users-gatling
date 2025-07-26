package redis;

import helpers.DataFormatHelper;
import helpers.ReadFileHelper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashMap;

@Slf4j
public class RedisInfo {
    private static final long[] units = {1_099_511_627_776L, 1_073_741_824L, 1_048_576L, 1024L};
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

        try (Jedis jedis = new Jedis(redisHost, redisPort)) {

            String info = jedis.info();
            String[] lines = info.split("\\r?\\n");
            String connectedClients = null;

            for (String line : lines) {
                if (line.startsWith("connected_clients")) {
                    connectedClients = line.split(":")[1].trim();
                    break;
                }
            }

            String infoMemory = jedis.info("memory");
            String[] memoryLines = infoMemory.split("\\r?\\n");
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
            log.info("Connected Clients: {}", connectedClients != null ? connectedClients : "-");
            log.info("Total Memory: {}", totalMemory != null ? totalMemory : "-");
            log.info("Memory Used: {}", usedMemory != null ? usedMemory : "-");

            // Заголовок таблицы
            String header = String.format("| %-80.80s | %-6.6s | %-21.21s | %-13.13s | %-27.27s |",
                    "Key", "Type", "Count", "Mem Usage", "TTL");
            String separator = "+----------------------------------------------------------------------------------+--------+-----------------------+---------------+-----------------------------+";

            log.info(separator);
            log.info(header);
            log.info(separator);

            String cursor = "0";
            ScanParams scanParams = new ScanParams().match("*").count(100);

            do {
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                cursor = scanResult.getCursor();

                for (String key : scanResult.getResult()) {
                    String type = jedis.type(key);
                    String count = switch (type) {
                        case "list" -> String.valueOf(jedis.llen(key));
                        case "set" -> String.valueOf(jedis.scard(key));
                        case "hash" -> String.valueOf(jedis.hlen(key));
                        case "zset" -> String.valueOf(jedis.zcard(key));
                        default -> "-";
                    };

                    Long memoryUsage = jedis.memoryUsage(key);
                    String sizeStr = memoryUsage != null ? formatSize(memoryUsage) : "-";

                    long ttlValue = jedis.ttl(key);
                    String ttlStr = ttlValue == -1 ? "-" : DataFormatHelper.format(ttlValue);

                    log.info(String.format("| %-80.80s | %-6.6s | %-21.21s | %-13.13s | %-27.27s |", key, type, count, sizeStr, ttlStr));
                }
            } while (!"0".equals(cursor));

            log.info(separator);

        } catch (Exception e) {
            log.error("Error while gathering Redis info", e);
        }
    }
}
