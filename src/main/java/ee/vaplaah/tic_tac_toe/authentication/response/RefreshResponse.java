package ee.vaplaah.tic_tac_toe.authentication.response;

import ee.vaplaah.tic_tac_toe.user.dto.AuthenticatedUserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresInSeconds;
    private AuthenticatedUserDto user;
}
