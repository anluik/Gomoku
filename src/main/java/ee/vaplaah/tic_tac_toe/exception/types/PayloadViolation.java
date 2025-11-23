package ee.vaplaah.tic_tac_toe.exception.types;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public class PayloadViolation {
    private String path;
    private String violation;
}
