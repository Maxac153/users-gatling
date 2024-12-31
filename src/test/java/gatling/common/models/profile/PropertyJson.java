package gatling.common.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PropertyJson {
    @SerializedName("COMMON_SETTINGS")
    private CommonSettings commonSettings;
    @SerializedName("TEST_PARAM")
    private TestParam testParam;
}
