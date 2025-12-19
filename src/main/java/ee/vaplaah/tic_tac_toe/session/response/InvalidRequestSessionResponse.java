package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.core.exception.types.RequestViolation;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
@Jacksonized
@SuperBuilder
public class InvalidRequestSessionResponse extends BaseSessionResponse {

    @Builder.Default
    List<RequestViolation> violations = new ArrayList<>();

    @Override
    public ResponseStatus getStatus() {
        return ResponseStatus.ERROR;
    }
}
