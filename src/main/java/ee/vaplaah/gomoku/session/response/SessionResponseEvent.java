package ee.vaplaah.gomoku.session.response;

public enum SessionResponseEvent {
    // ======== success responses ========
    USER_JOINED,                                // user joined a session
    GAME_STARTED,                               // both players joined; the game is now underway
    USER_RESIGNED,                              // user resigned
    MOVE_MADE,                                  // a player made a move
    GAME_WON,                                   // a move (or abandonment) ended the game with a winner
    GAME_DRAW,                                  // the board filled with no winner
    GAME_ABORTED,                               // game ended with no winner (e.g. abandoned before any move)
    PLAYER_DISCONNECTED,                        // a player's last connection dropped; forfeit timer started
    PLAYER_RECONNECTED,                         // a disconnected player returned within the grace period
    CHAT_MESSAGE,                               // a chat message was sent to the game

    // ======== game errors ========
    GAME_FULL,                                  // when trying to join a full game
    GAME_NOT_FOUND,                             // game does not exist
    GAME_ALREADY_OVER,                          // when trying to perform an action on a game that is already over
    USER_NOT_PART_OF_THE_GAME,                  // when a user tries to perform an action in a game they are not part of
    USER_ALREADY_JOINED,                        // when a user tries join a game that they are already part of
    UNSUPPORTED_EVENT,                          // event type the server has no handler for
    GAME_NOT_READY,                             // a move was attempted before both players joined
    NOT_YOUR_TURN,                              // a move was attempted out of turn
    INVALID_MOVE,                               // a move was out of bounds, on a taken cell, or stale
    ABORT_NOT_ALLOWED,                          // abort attempted after the player had already moved
    RESIGN_NOT_ALLOWED,                         // resign attempted before the player had made a move

    // ======== protocol errors ========
    MALFORMED_PAYLOAD,                          // payload could not be parsed or had the wrong shape
    VALIDATION_FAILED,                          // payload failed bean-validation; data holds the violations
    UNEXPECTED_ERROR,                           // any error not covered by other enums
}
