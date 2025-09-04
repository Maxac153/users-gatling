package ru.gatling.__common.helpers;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.http.HttpDsl;

import static io.gatling.javaapi.core.CoreDsl.regex;

public class StsHelper {
    public static ChainBuilder status = CoreDsl.exec(
            HttpDsl.http("ur_sts_status")
                    .get("/sts/STATUS")
                    .check(regex("").saveAs("status"))
                    .check(regex("<body>([^<]*)<br>").saveAs("line"))
    );

    public static ChainBuilder add = CoreDsl.exec(
            HttpDsl.http("ur_sts_add")
                    .get("/sts/READ")
                    .queryParam("ADD_MODE", "LAST")
                    .queryParam("UNIQUE", "FALSE")
                    .queryParam("FILENAME", "#{FILE_NAME_ADD}")
                    .queryParam("LINE", "#{line}")
    );

    public static ChainBuilder read = CoreDsl.exec(
            HttpDsl.http("ur_sts_read")
                    .get("/sts/READ")
                    .queryParam("READ_MODE", "FIRST")
                    .queryParam("KEEP", "FALSE")
                    .queryParam("FILENAME", "#{FILE_NAME_READ}")
                    .check(regex("<body>([^<]*)<br>").saveAs("line"))
    );

    public static ChainBuilder reset = CoreDsl.exec(
            HttpDsl.http("ur_sts_reset")
                    .get("/sts/RESET")
                    .queryParam("FILENAME", "#{FILE_NAME_RESET}")
    );
}
