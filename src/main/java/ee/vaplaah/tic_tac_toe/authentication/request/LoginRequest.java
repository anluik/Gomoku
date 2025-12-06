package ee.vaplaah.tic_tac_toe.authentication.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;

@Getter
public class LoginRequest {

    @NotEmpty
    private String username;
    @NotEmpty
    private String password;
}
