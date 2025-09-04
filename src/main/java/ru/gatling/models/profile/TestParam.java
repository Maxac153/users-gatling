package ru.gatling.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class TestParam {
    @SerializedName("RUN")
    private Run run;
    @SerializedName("PROFILE")
    private List<Profile> profiles;
    @SerializedName("PROPERTIES")
    private HashMap<String, Object> properties;
}
