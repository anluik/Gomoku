package ee.vaplaah.tic_tac_toe.session.response;

public enum SessionResponseEvent {
    // ======== success responses ========
    USER_JOINED,                                // user joined a session
    USER_LEFT,                                  // user left a session

    // ======== error responses ========
    GAME_FULL,                                  // when trying to join a full game
    INVALID_PAYLOAD,                            // payload that conflicts with validation rules or system state
    UNEXPECTED_ERROR,                           // any error not covered by other enums
}
