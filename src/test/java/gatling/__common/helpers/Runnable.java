package gatling.__common.helpers;

import io.gatling.javaapi.core.PopulationBuilder;
import models.profile.Profile;

import java.util.ArrayList;
import java.util.Map;

public interface Runnable {
    PopulationBuilder run(Map<String, Object> testSettings, ArrayList<Profile> testProfile);
}
