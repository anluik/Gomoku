package ee.vaplaah.tic_tac_toe.session.response;

public enum SessionResponseEvent {
    // ======== success responses ========
    USER_JOINED,                                // user joined a session
    USER_LEFT,                                  // user left a session
    USER_RESIGNED,                              // user resigned

    // ======== error responses ========
    GAME_FULL,                                  // when trying to join a full game
    GAME_NOT_FOUND,                             // game does not exist
    GAME_ALREADY_OVER,                          // when trying to perform an action on a game that is already over
    USER_NOT_PART_OF_THE_GAME,                  // when a user tries to perform an action in a game they are not part of
    INVALID_PAYLOAD,                            // payload that conflicts with validation rules or system state
    UNEXPECTED_ERROR,                           // any error not covered by other enums
}
