package ru.gatling.users.t2_registration.helper;

import com.google.gson.Gson;
import net.datafaker.Faker;
import ru.gatling.__common.models.login.Registration;

public class RegistrationUserRandomHelper {
    private static final Gson GSON = new Gson();
    private static final Faker FAKER = new Faker();

    public static String get() {
        return GSON.toJson(Registration.builder()
                .name(FAKER.name().fullName())
                .email(FAKER.internet().emailAddress())
                .password(FAKER.internet().password())
                .build()
        );
    }
}
