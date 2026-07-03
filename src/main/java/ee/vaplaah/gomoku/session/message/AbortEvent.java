package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;

public class AbortEvent extends GameEvent {

    public AbortEvent() {
        super(GameEventType.ABORT);
    }
}
