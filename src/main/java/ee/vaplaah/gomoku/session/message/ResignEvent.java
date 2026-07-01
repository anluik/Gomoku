package ee.vaplaah.gomoku.session.message;

import ee.vaplaah.gomoku.game.session.GameEventType;

public class ResignEvent extends GameEvent {

    public ResignEvent() {
        super(GameEventType.RESIGN);
    }
}
