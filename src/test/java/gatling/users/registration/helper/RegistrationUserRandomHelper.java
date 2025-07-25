package gatling.users.registration.helper;

import com.google.gson.Gson;
import gatling.__common.models.login.Registration;
import net.datafaker.Faker;

public class RegistrationUserRandomHelper {
    private static final Gson gson = new Gson();
    private static final Faker faker = new Faker();

    public static String get() {
        return gson.toJson(Registration.builder()
                .name(faker.name().fullName())
                .email(faker.internet().emailAddress())
                .password(faker.internet().password())
                .build()
        );
    }
}
