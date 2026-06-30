package ee.vaplaah.gomoku.core.exception.authentication;

import ee.vaplaah.gomoku.core.exception.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

/**
 * Base class for all custom authentication-related exceptions.
 * Extends AuthenticationException so that the exceptions would be caught by AuthenticationFailureHandler.
 */
@Getter
public class BaseAuthenticationException extends AuthenticationException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    public BaseAuthenticationException(String message, HttpStatus httpStatus, ErrorCode errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BaseAuthenticationException(HttpStatus httpStatus, ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}