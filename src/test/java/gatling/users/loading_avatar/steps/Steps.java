package gatling.users.loading_avatar.steps;


import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.util.Random;

import static io.gatling.javaapi.http.HttpDsl.RawFileBodyPart;

public class Steps {
    private static final Random rand = new Random();
    private static final String[] avatars = {
            "tests/users/loading_avatar/images/g-12.jpg",
            "tests/users/loading_avatar/images/worm.jpg"
    };

    private static String getRandomAvatar() {
        return avatars[rand.nextInt(avatars.length)];
    }

    public static HttpRequestActionBuilder loadingAvatar = HttpDsl.http("ur_loading_avatar")
            .post("/tasks/rest/addavatar")
            .queryParam("email", "#{email}")
            .bodyPart(RawFileBodyPart("avatar", getRandomAvatar()).contentType("image/jpeg"));

    public static HttpRequestActionBuilder deleteAvatar = HttpDsl.http("ur_delete_avatar")
            .delete("/tasks/rest/deleteavatar")
            .queryParam("email", "#{email}");
}
