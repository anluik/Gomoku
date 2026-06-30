package ee.vaplaah.gomoku.core.exception.authentication;

import io.jsonwebtoken.Claims;
import lombok.Getter;

import static ee.vaplaah.gomoku.core.exception.enums.ErrorCode.TOKEN_EXPIRED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Thrown when an Access or Refresh Token has passed its expiry date.
 */
@Getter
public class TokenExpiredException extends BaseAuthenticationException {

    private Claims claims;

    public TokenExpiredException(String message) {
        super(message, UNAUTHORIZED, TOKEN_EXPIRED);
    }

    public TokenExpiredException() {
        super(UNAUTHORIZED, TOKEN_EXPIRED);
    }

    public TokenExpiredException(Claims claims) {
        super(UNAUTHORIZED, TOKEN_EXPIRED);
        this.claims = claims;
    }
}