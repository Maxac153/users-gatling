package gatling.common.models.redis;

import lombok.Getter;

@Getter
public enum RedisReadType {
    LAST("LAST"),
    FIRST("FIRST");

    private final String readMode;

    RedisReadType(String readMode) {
        this.readMode = readMode;
    }
}
