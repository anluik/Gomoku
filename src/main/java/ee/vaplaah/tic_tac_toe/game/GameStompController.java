package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.stomp.GameEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameStompController {

    @MessageMapping("/game.joinGame")
    @SendTo("/topic/game")
    public GameEvent joinGame(@Payload GameEvent gameEventType) {
        return gameEventType;
    }

    @MessageMapping("/game.makeMove")
    @SendTo("/topic/game")
    public GameEvent makeMove(@Payload GameEvent gameEventType) {
        return gameEventType;
    }
}
