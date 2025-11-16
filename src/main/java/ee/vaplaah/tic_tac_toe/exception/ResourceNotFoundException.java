package ee.vaplaah.tic_tac_toe.exception;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class ResourceNotFoundException extends CommonException {

    public ResourceNotFoundException() {
        super("Resource not found", NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, NOT_FOUND);
    }
}
