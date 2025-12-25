package ee.vaplaah.tic_tac_toe.core.exception;

import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import lombok.Getter;

public class SessionMessageProcessingException extends RuntimeException {

    @Getter
    private final BaseSessionResponse<?> response;

    public SessionMessageProcessingException(BaseSessionResponse<?> response) {
        super(response.getMessage());
        this.response = response;
    }
}
