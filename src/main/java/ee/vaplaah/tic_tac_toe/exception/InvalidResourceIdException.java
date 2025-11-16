package ee.vaplaah.tic_tac_toe.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class InvalidResourceIdException extends CommonException {

    public InvalidResourceIdException() {
        super("Provided ID is not valid", BAD_REQUEST);
    }

    public InvalidResourceIdException(String message) {
        super(message, BAD_REQUEST);
    }
}
