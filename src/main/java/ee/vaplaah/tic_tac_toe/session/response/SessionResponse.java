package ee.vaplaah.tic_tac_toe.session.response;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse<T> {

    private String gameId;
    private ResponseStatus status;
    private SessionResponseEvent responseEvent;
    private String message; // Developer-facing diagnostic text, not for display in UI.
    private T data;

    public static <T> SessionResponseBuilder<T> success(SessionResponseEvent event, String message) {
        return SessionResponse.<T>builder()
            .status(ResponseStatus.SUCCESS)
            .responseEvent(event)
            .message(message);
    }

    public static <T> SessionResponseBuilder<T> error(SessionResponseEvent event, String message) {
        return SessionResponse.<T>builder()
            .status(ResponseStatus.ERROR)
            .responseEvent(event)
            .message(message);
    }
}
