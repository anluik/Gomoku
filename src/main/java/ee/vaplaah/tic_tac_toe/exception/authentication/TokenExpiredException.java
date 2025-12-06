package ee.vaplaah.tic_tac_toe.exception.authentication;

import static ee.vaplaah.tic_tac_toe.enums.ErrorCode.TOKEN_EXPIRED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Thrown when an Access or Refresh Token has passed its expiry date.
 */
public class TokenExpiredException extends BaseAuthenticationException {

    public TokenExpiredException(String message) {
        super(message, UNAUTHORIZED, TOKEN_EXPIRED);
    }

    public TokenExpiredException() {
        super(UNAUTHORIZED, TOKEN_EXPIRED);
    }
}