package ee.vaplaah.gomoku.core.exception;

import ee.vaplaah.gomoku.session.response.SessionResponse;
import lombok.Getter;

public class SessionMessageProcessingException extends RuntimeException {

    @Getter
    private final SessionResponse<?> response;

    public SessionMessageProcessingException(SessionResponse<?> response) {
        super(response.getMessage());
        this.response = response;
    }
}
