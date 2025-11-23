package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.exception.types.PayloadViolation;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.HashSet;
import java.util.Set;

@Jacksonized
@SuperBuilder
public class InvalidPayloadResponse extends BaseResponse {

    @Builder.Default
    Set<PayloadViolation> violations = new HashSet<>();

    @Override
    public ResponseStatus getStatus() {
        return ResponseStatus.ERROR;
    }
}
