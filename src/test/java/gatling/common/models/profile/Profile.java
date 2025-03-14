package gatling.common.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Profile {
    @SerializedName("SCENARIO_NAME")
    private String scenarioName;
    @SerializedName("SCRIPT_EXECUTION_TIME")
    private Long scriptExecutionTime;
    @SerializedName("STEPS")
    private ArrayList<Step> steps;
}
