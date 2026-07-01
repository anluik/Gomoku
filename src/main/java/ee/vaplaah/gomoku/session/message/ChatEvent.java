package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatEvent extends GameEvent {

    @NotBlank(message = "Chat message must not be blank")
    @Size(max = 500, message = "Chat message too long")
    private String text;

    public ChatEvent() {
        super(GameEventType.CHAT);
    }
}
