package gatling.common.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Profile {
    @SerializedName("SCENARIO_NAME")
    private String scenarioName;
    @SerializedName("STEPS")
    private ArrayList<Step> steps;
}
