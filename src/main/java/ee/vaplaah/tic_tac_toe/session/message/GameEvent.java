package ee.vaplaah.tic_tac_toe.session.message;

import ee.vaplaah.tic_tac_toe.enums.GameEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class GameEvent {

    @NotNull(message = "Game ID not specified")
    private Long gameId;

    @NotNull(message = "Event type not specified")
    private GameEventType type;
}
