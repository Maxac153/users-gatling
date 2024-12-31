package gatling.common.models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class TestParam {
    @SerializedName("PROFILE")
    private ArrayList<Profile> profile;
    @SerializedName("PROPERTIES")
    private HashMap<String, String> properties;
}
