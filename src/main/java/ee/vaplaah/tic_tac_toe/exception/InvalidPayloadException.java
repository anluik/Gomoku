package ee.vaplaah.tic_tac_toe.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class InvalidPayloadException extends CommonException {

    public InvalidPayloadException() {
        super("Invalid request payload", BAD_REQUEST);
    }

    public InvalidPayloadException(String message) {
        super(message, BAD_REQUEST);
    }
}
