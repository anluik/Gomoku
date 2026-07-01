package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;

public class LeaveGameEvent extends GameEvent {

    public LeaveGameEvent() {
        super(GameEventType.LEAVE_GAME);
    }
}
