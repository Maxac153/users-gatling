package gatling.__common.models.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Authorization {
    private String login;
    private String password;
}
