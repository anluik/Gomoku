package ee.vaplaah.gomoku.session.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import ee.vaplaah.gomoku.game.session.GameEventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * Base type for every incoming WebSocket command for a game.
 */
@Getter
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @Type(value = JoinGameEvent.class, name = "JOIN_GAME"),
    @Type(value = LeaveGameEvent.class, name = "LEAVE_GAME"),
    @Type(value = ResignEvent.class, name = "RESIGN"),
    @Type(value = StartGameEvent.class, name = "START_GAME"),
    @Type(value = MoveEvent.class, name = "MOVE"),
    @Type(value = ChatEvent.class, name = "CHAT"),
})
public abstract class GameEvent {

    @NotNull(message = "Event type not specified")
    private GameEventType type;

    protected GameEvent() {
    }

    protected GameEvent(GameEventType type) {
        this.type = type;
    }
}
