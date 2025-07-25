package models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class BuildSettings {
    @SerializedName("MODULE_NAME")
    private String moduleName;
    @SerializedName("LEVEL_CONSOLE_LOG")
    private String levelConsoleLog;
    @SerializedName("LEVEL_FILE_LOG")
    private String levelFileLog;
    @SerializedName("PERCENT_PROFILE")
    private Double percentProfile;
}
