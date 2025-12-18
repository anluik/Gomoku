package ee.vaplaah.tic_tac_toe.core.exception;

import static ee.vaplaah.tic_tac_toe.core.exception.enums.ErrorCode.RESOURCE_NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_FOUND;

public class ResourceNotFoundException extends CommonException {

    public ResourceNotFoundException() {
        super(NOT_FOUND, RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, NOT_FOUND, RESOURCE_NOT_FOUND);
    }
}
