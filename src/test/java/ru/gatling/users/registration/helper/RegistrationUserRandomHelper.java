package ru.gatling.users.registration.helper;

import com.google.gson.Gson;
import net.datafaker.Faker;
import ru.gatling.__common.models.login.Registration;

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
