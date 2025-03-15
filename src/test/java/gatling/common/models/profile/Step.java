package gatling.common.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Step {
    @SerializedName("TPS")
    private Double tps;
    @SerializedName("RAMP_TIME")
    private Double rampTime;
    @SerializedName("HOLD_TIME")
    private Double holdTime;
}
