package gatling.common.helpers;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.http.HttpDsl;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.status;

public class RedisClientHelper {
    public static ChainBuilder readList = CoreDsl.exec(
            HttpDsl.http("db_read_data_redis_list")
                    .get("#{REDIS_CLIENT_PROTOCOL}://#{REDIS_CLIENT_HOST}:#{REDIS_CLIENT_PORT}/readVecDeque")
                    .queryParam("key", "#{REDIS_KEY_READ}")
                    .queryParam("read_mode", "#{redis_read_mode}")
                    .check(status().is(200))
                    .check(jsonPath("#.data").saveAs("redis_data"))
    );

    public static ChainBuilder addList = CoreDsl.exec(
            HttpDsl.http("db_add_data_redis_list")
                    .post("#{REDIS_CLIENT_PROTOCOL}://#{REDIS_CLIENT_HOST}:#{REDIS_CLIENT_PORT}/addVecDeque")
                    .header("Content-Type", "application/json")
                    .queryParam("key", "#{REDIS_KEY_ADD}")
                    .queryParam("add_mode", "#{redis_add_mode}")
                    .body(StringBody(session -> session.getString("json"))).asJson()
                    .check(status().is(201))

    );
}
