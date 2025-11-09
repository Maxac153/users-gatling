package ru.gatling.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Run {
    @SerializedName("GENERATOR")
    private String generator;
    @SerializedName("ENV")
    private String env;
    @SerializedName("SIMULATION_CLASS")
    private String simulationClass;
}
