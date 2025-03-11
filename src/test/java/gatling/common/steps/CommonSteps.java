package gatling.common.steps;


import com.google.gson.Gson;
import gatling.common.models.login.Authorization;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static io.gatling.javaapi.core.CoreDsl.StringBody;

public class CommonSteps {
    private static final Gson gson = new Gson();

    public static HttpRequestActionBuilder mainPage =
            HttpDsl.http("ur_open_main_page")
                    .get("/");

    public static HttpRequestActionBuilder logout = HttpDsl.http("ur_logout")
            .get("/user/logout.html");

    public static HttpRequestActionBuilder authorization =
            HttpDsl.http("ur_authentication")
                    .post("/user/login/index.html")
                    .header("Content-Type", "application/json")
                    .body(StringBody(session -> session.getString("redis_data"))).asJson();

    public static HttpRequestActionBuilder authorizationAdmin = HttpDsl.http("ur_authentication")
            .post("/user/login/index.html")
            .header("Content-Type", "application/json")
            .body(StringBody(session ->
                            gson.toJson(
                                    Authorization.builder()
                                            .login(session.getString("login"))
                                            .password(session.getString("password"))
                                            .build()
                            )
                    )
            ).asJson();

    public static HttpRequestActionBuilder searchUser = HttpDsl.http("ur_search_user")
            .get("/user/admin/index")
            .formParam("date_start", "")
            .formParam("date_end", "")
            .formParam("d", "#{user_name}");

    public static HttpRequestActionBuilder deleteUser = HttpDsl.http("ur_delete_user")
            .get("/user/admin/index")
            .formParam("q", "#{user_name}")
            .formParam("date_start", "")
            .formParam("date_end", "")
            .formParam("path_grid_asd", "/delete/#{user_id}");
}
