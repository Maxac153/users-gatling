package ru.gatling.users.registration.steps;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.http.HttpDsl;

import static io.gatling.javaapi.core.CoreDsl.StringBody;

public class Steps {
    public static ChainBuilder registration = CoreDsl.exec(
            HttpDsl.http("ur_registration")
                    .post("/tasks/rest/doregister")
                    .body(StringBody(session -> session.getString("json"))).asJson()
    );
}
