package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.enums.ResponseStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse {

    private ResponseStatus status;
    private String message;
}
