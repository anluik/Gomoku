package ee.vaplaah.gomoku.authentication.response;

import ee.vaplaah.gomoku.user.dto.AuthenticatedUserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private boolean authenticated;
    @NonNull
    private Long expiresInSeconds;
    @NonNull
    private AuthenticatedUserDto user;
}
