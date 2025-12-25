package ee.vaplaah.tic_tac_toe.session.response;

public enum SessionResponseEvent {
    // success response
    SUCCESS, // TODO: split this

    // error response
    INVALID_PAYLOAD,
    UNEXPECTED_ERROR,
}
