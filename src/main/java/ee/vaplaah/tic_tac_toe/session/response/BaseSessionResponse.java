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

    // ========== Common Error Responses ==========

    public static BaseSessionResponse<?> ofGameNotFound() {
        return BaseSessionResponse.builder()
            .status(ResponseStatus.ERROR)
            .responseEvent(SessionResponseEvent.GAME_NOT_FOUND)
            .message("Game not found")
            .build();
    }

    public static BaseSessionResponse<?> ofGameAlreadyOver() {
        return BaseSessionResponse.builder()
            .status(ResponseStatus.ERROR)
            .responseEvent(SessionResponseEvent.GAME_ALREADY_OVER)
            .message("Game has ended")
            .build();
    }

    public static BaseSessionResponse<?> ofUserNotPartOfTheGame() {
        return BaseSessionResponse.builder()
            .status(ResponseStatus.ERROR)
            .responseEvent(SessionResponseEvent.USER_NOT_PART_OF_THE_GAME)
            .message("User not part of the game")
            .build();
    }
}
