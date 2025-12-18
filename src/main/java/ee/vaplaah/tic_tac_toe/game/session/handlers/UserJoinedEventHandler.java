package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;

public class UserJoinedEventHandler implements GameEventHandler {

    @Override
    public void handle(GameEvent event) {
        // 1. get game from DB
        // 2. check that there aren't more than 2 players
        // 3. update players list
        // 4. broadcast user joined event
    }

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.USER_JOINED;
    }
}
