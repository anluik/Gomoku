package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;

public class JoinGameEvent extends GameEvent {

    public JoinGameEvent() {
        super(GameEventType.JOIN_GAME);
    }
}
