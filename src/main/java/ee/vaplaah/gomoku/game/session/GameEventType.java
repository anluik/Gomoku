package ee.vaplaah.gomoku.game.session;

public enum GameEventType {
    JOIN_GAME,
    RESIGN,
    // TODO: how would this be used? Once server determines that the game can begin, the state will be updated and players notified.
    // It could be an acknowledgement event that client received the message to start the game.
    START_GAME,
    MOVE,
    CHAT,
    ;
}
