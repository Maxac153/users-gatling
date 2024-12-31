package gatling.common.models.redis;

import lombok.Getter;

@Getter
public enum RedisAddType {
    LAST("LAST"),
    FIRST("FIRST");

    private final String addMode;

    RedisAddType(String addMode) {
        this.addMode = addMode;
    }
}
