package models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Profile {
    @SerializedName("SCENARIO_NAME")
    private String scenarioName;
    @SerializedName("PACING")
    private Long pacing;
    @SerializedName("STEPS")
    private ArrayList<Step> steps;
}
