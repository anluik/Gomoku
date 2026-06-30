package ee.vaplaah.gomoku.core.exception;

import ee.vaplaah.gomoku.core.exception.enums.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import static ee.vaplaah.gomoku.core.exception.enums.ErrorCode.INVALID_REQUEST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@Setter
public class CommonException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final Object details;

    public CommonException(String message) {
        super(message);
        this.httpStatus = BAD_REQUEST;
        this.errorCode = INVALID_REQUEST;
        this.details = null;
    }

    public CommonException(HttpStatus httpStatus, ErrorCode errorCode) {
        this(errorCode.getMessage(), httpStatus, errorCode);
    }

    public CommonException(HttpStatus httpStatus, ErrorCode errorCode, Object details) {
        this(errorCode.getMessage(), httpStatus, errorCode, details);
    }

    public CommonException(String message, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = null;
    }

    public CommonException(String message, HttpStatus httpStatus, ErrorCode errorCode, Object details) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.details = details;
    }
}
