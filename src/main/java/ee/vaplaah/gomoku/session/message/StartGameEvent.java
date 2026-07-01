package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;

public class StartGameEvent extends GameEvent {

    public StartGameEvent() {
        super(GameEventType.START_GAME);
    }
}
