package ee.vaplaah.gomoku.session.response.game.payload;

import ee.vaplaah.gomoku.game.Move;

public record MoveData(Move move, int moveCount) {
}
