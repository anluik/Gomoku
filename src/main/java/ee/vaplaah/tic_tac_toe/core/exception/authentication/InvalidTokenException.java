package ee.vaplaah.tic_tac_toe.core.exception.authentication;

import static ee.vaplaah.tic_tac_toe.core.exception.enums.ErrorCode.INVALID_TOKEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Thrown when an Access Token is invalid due to signature mismatch,
 * wrong format, or general malformation.
 */
public class InvalidTokenException extends BaseAuthenticationException {

    public InvalidTokenException(String message) {
        super(message, UNAUTHORIZED, INVALID_TOKEN);
    }

    public InvalidTokenException() {
        super(UNAUTHORIZED, INVALID_TOKEN);
    }
}