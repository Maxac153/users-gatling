package models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;

@Data
public class CommonSettings {
    @SerializedName("RUN_SETTINGS")
    private RunSettings runSettings;
    @SerializedName("PROPERTIES")
    private HashMap<String, Object> properties;
}
