package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;

public interface GameEventHandler {

    void handle(GameEvent event);
    boolean supports(GameEventType eventType);
}
