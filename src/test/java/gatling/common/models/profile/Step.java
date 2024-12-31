package gatling.common.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Step {
    @SerializedName("STAR_TPS")
    private Double startTps;
    @SerializedName("END_TPS")
    private Double endTps;
    @SerializedName("RAMP_TIME")
    private Double rampTime;
    @SerializedName("HOLD_TIME")
    private Double holdTime;
}
