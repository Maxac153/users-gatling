package models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;

@Data
public class CommonSettings {
    @SerializedName("BUILD_SETTINGS")
    private BuildSettings buildSettings;
    @SerializedName("PROPERTIES")
    private HashMap<String, Object> properties;
}
