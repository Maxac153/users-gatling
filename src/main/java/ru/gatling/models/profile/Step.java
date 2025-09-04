package ru.gatling.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class Step {
    @SerializedName("TPS")
    private Double tps;
    @SerializedName("RAMP_TIME")
    private Double rampTime;
    @SerializedName("HOLD_TIME")
    private Double holdTime;
}
