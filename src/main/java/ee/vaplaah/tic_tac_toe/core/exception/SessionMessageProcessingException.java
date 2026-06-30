package ee.vaplaah.tic_tac_toe.core.exception;

import ee.vaplaah.tic_tac_toe.session.response.SessionResponse;
import lombok.Getter;

public class SessionMessageProcessingException extends RuntimeException {

    @Getter
    private final SessionResponse<?> response;

    public SessionMessageProcessingException(SessionResponse<?> response) {
        super(response.getMessage());
        this.response = response;
    }
}
