package ee.vaplaah.tic_tac_toe.session.message;

import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameEvent {

    @NotNull(message = "Game ID not specified")
    private String gameId;

    @NotNull(message = "Event type not specified")
    private GameEventType type;
}
