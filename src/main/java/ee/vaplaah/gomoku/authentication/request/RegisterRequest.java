package ee.vaplaah.gomoku.authentication.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotEmpty
    private String username;
    @NotEmpty
    private String password;
}
