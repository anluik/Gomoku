package ee.vaplaah.tic_tac_toe.core.exception.authentication;

import static ee.vaplaah.tic_tac_toe.core.exception.enums.ErrorCode.USERNAME_TAKEN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class UsernameTakenException extends BaseAuthenticationException {

    public UsernameTakenException(String message) {
        super(message, BAD_REQUEST, USERNAME_TAKEN);
    }

    public UsernameTakenException() {
        super(BAD_REQUEST, USERNAME_TAKEN);
    }
}