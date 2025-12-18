package ee.vaplaah.tic_tac_toe.core.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // authentication
    INVALID_TOKEN("Invalid credentials - access denied"),
    TOKEN_EXPIRED("Session has expired - log in again"),
    USERNAME_TAKEN("Username is already taken"),
    INVALID_CREDENTIALS("Invalid credentials"),

    // client errors
    INVALID_REQUEST("Unable to process the request"),
    RESOURCE_NOT_FOUND("Resource does not exist"),
    ;

    private final String message;
}
