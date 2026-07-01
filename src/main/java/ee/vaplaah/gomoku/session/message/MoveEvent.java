package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MoveEvent extends GameEvent {

    @NotNull(message = "Move x coordinate not specified")
    private Integer x;

    @NotNull(message = "Move y coordinate not specified")
    private Integer y;

    // TODO: check how this would be useful
    /**
     * Client-assigned sequence number, expected to equal the current move count. Lets the server
     * reject stale or duplicated moves (idempotency), which also makes at-least-once delivery safe
     * when external transport is introduced later.
     */
    private long moveSeq;

    public MoveEvent() {
        super(GameEventType.MOVE);
    }
}
