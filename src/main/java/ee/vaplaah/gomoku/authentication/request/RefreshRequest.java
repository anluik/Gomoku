package ee.vaplaah.gomoku.authentication.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotEmpty
    private String refreshToken;
}
