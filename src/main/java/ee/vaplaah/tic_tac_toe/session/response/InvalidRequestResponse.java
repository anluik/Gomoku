package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.exception.types.RequestViolation;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Jacksonized
@SuperBuilder
@Getter
public class InvalidRequestResponse extends BaseResponse {

    @Builder.Default
    List<RequestViolation> violations = new ArrayList<>();

    @Override
    public ResponseStatus getStatus() {
        return ResponseStatus.ERROR;
    }
}
