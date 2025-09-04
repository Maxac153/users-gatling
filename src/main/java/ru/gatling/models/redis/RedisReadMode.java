package ru.gatling.models.redis;

import lombok.Getter;

@Getter
public enum RedisReadMode {
    LAST,
    FIRST
}
