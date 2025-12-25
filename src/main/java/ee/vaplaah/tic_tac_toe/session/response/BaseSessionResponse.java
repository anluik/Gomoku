package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseSessionResponse<T> {

    private ResponseStatus status;
    private SessionResponseEvent responseEvent;
    private String message;
    protected T data;
}
