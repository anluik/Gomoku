package ee.vaplaah.tic_tac_toe.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@Setter
public class CommonException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final List<FieldError> fieldErrors;

    public CommonException(String message) {
        super(message);
        this.fieldErrors = new ArrayList<>();
        this.httpStatus = BAD_REQUEST;
    }

    public CommonException(String message, HttpStatus httpStatus) {
        super(message);
        this.fieldErrors = new ArrayList<>();
        this.httpStatus = httpStatus;
    }

    public CommonException(String message, List<FieldError> fieldErrors, HttpStatus httpStatus) {
        super(message);
        this.fieldErrors = fieldErrors;
        this.httpStatus = httpStatus;
    }
}
