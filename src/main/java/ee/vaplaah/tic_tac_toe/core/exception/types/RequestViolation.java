package ee.vaplaah.tic_tac_toe.core.exception.types;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class RequestViolation {
    private String field;
    private String error;
}
