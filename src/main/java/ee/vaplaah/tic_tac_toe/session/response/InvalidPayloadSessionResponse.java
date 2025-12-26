package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.core.exception.types.RequestViolation;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Jacksonized
@SuperBuilder
public class InvalidPayloadSessionResponse extends BaseSessionResponse<List<RequestViolation>> {

    @Override
    public SessionResponseEvent getResponseEvent() {
        return SessionResponseEvent.INVALID_PAYLOAD;
    }

    @Override
    public ResponseStatus getStatus() {
        return ResponseStatus.ERROR;
    }

    @Override
    public List<RequestViolation> getData() {
        return data != null ? data : List.of();
    }

    public static InvalidPayloadSessionResponse withMessage(String message) {
        return InvalidPayloadSessionResponse.builder()
                .message(message)
                .build();
    }
}
