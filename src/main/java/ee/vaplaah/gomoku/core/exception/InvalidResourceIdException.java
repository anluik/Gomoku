package ee.vaplaah.gomoku.core.exception;

import static ee.vaplaah.gomoku.core.exception.enums.ErrorCode.INVALID_REQUEST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class InvalidResourceIdException extends CommonException {

    public InvalidResourceIdException() {
        super("Provided ID is not valid", BAD_REQUEST, INVALID_REQUEST);
    }

    public InvalidResourceIdException(String message) {
        super(message, BAD_REQUEST, INVALID_REQUEST);
    }
}
