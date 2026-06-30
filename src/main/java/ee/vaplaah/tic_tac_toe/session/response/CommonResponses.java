package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.core.exception.types.RequestViolation;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class CommonResponses {

    public SessionResponse<Void> unexpected() {
        return SessionResponse.<Void>error(SessionResponseEvent.UNEXPECTED_ERROR, "Unexpected Server Error").build();
    }

    public SessionResponse<Void> malformedPayload(String message) {
        return SessionResponse.<Void>error(SessionResponseEvent.MALFORMED_PAYLOAD, message).build();
    }

    public SessionResponse<List<RequestViolation>> validationFailed(List<RequestViolation> violations) {
        return SessionResponse.<List<RequestViolation>>error(SessionResponseEvent.VALIDATION_FAILED, "Validation failed")
            .data(violations != null ? violations : List.of())
            .build();
    }
}
