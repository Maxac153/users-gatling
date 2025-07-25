package models.profile;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Elements {
    @SerializedName("profile")
    private TestParam testParam;
}
