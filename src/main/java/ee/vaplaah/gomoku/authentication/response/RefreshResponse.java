package ee.vaplaah.gomoku.authentication.response;

import ee.vaplaah.gomoku.user.dto.AuthenticatedUserDto;
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
