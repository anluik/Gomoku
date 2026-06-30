package ee.vaplaah.gomoku.core.exception.authentication;

import static ee.vaplaah.gomoku.core.exception.enums.ErrorCode.INVALID_TOKEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class InvalidCredentialsException extends BaseAuthenticationException {

    public InvalidCredentialsException(String message) {
        super(message, UNAUTHORIZED, INVALID_TOKEN);
    }

    public InvalidCredentialsException() {
        super(UNAUTHORIZED, INVALID_TOKEN);
    }
}