package ru.gatling.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RunSettings {
    @SerializedName("BUILD_JAR_ENABLE")
    Boolean buildJarEnable;
    @SerializedName("MODULE_NAME")
    private String moduleName;
    @SerializedName("LEVEL_CONSOLE_LOG")
    private String levelConsoleLog;
    @SerializedName("LEVEL_FILE_LOG")
    private String levelFileLog;
    @SerializedName("DATASOURCE_HOST")
    String datasourceHost;
    @SerializedName("DATASOURCE_PORT")
    Integer datasourcePort;
    @SerializedName("PERCENT_PROFILE")
    private Double percentProfile;
}
