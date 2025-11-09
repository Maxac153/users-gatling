package ru.gatling.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;

@Data
public class TestsParam {
    @SerializedName("TESTS_PARAM")
    private HashMap<String, TestParam> testParam;
    @SerializedName("COMMON_SETTINGS")
    private CommonSettings commonSettings;
}
