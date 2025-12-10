package ee.vaplaah.tic_tac_toe.exception;

import ee.vaplaah.tic_tac_toe.enums.ErrorCode;
import ee.vaplaah.tic_tac_toe.exception.types.RequestViolation;
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
    private final List<RequestViolation> violations;

    public CommonException(String message) {
        super(message);
        this.violations = new ArrayList<>();
        this.httpStatus = BAD_REQUEST;
        this.errorCode = INVALID_REQUEST;
    }

    public CommonException(HttpStatus httpStatus, ErrorCode errorCode) {
        this(errorCode.getMessage(), httpStatus, errorCode);
    }

    public CommonException(List<RequestViolation> violations, HttpStatus httpStatus, ErrorCode errorCode) {
        this(errorCode.getMessage(), violations, httpStatus, errorCode);
    }

    public CommonException(String message, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.violations = new ArrayList<>();
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public CommonException(String message, List<RequestViolation> violations, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.violations = violations;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
