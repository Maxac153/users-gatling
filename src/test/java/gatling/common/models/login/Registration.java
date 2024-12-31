package gatling.common.models.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Registration {
    private String email;
    private String name;
    private String password;
}
