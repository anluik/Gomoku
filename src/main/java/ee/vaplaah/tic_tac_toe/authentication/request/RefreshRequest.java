package ee.vaplaah.tic_tac_toe.authentication.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotEmpty
    private String refreshToken;
}
