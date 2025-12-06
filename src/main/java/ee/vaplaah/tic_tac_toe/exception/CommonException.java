package ee.vaplaah.tic_tac_toe.exception;

import ee.vaplaah.tic_tac_toe.enums.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static ee.vaplaah.tic_tac_toe.enums.ErrorCode.INVALID_REQUEST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@Setter
public class CommonException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final List<FieldError> fieldErrors;

    public CommonException(String message) {
        super(message);
        this.fieldErrors = new ArrayList<>();
        this.httpStatus = BAD_REQUEST;
        this.errorCode = INVALID_REQUEST;
    }

    public CommonException(HttpStatus httpStatus, ErrorCode errorCode) {
        this(errorCode.getMessage(), httpStatus, errorCode);
    }

    public CommonException(List<FieldError> fieldErrors, HttpStatus httpStatus, ErrorCode errorCode) {
        this(errorCode.getMessage(), fieldErrors, httpStatus, errorCode);
    }

    public CommonException(String message, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.fieldErrors = new ArrayList<>();
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public CommonException(String message, List<FieldError> fieldErrors, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.fieldErrors = fieldErrors;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
