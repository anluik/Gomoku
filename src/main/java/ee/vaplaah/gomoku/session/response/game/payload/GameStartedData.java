package ee.vaplaah.gomoku.session.response.game.payload;

import ee.vaplaah.gomoku.user.UserIdAndName;

import java.util.List;

/**
 * Broadcast when both players have joined and the game is underway. {@code firstPlayerId} is the
 * player who moves first (player[0]), matching the turn order enforced by the move handler.
 */
public record GameStartedData(List<UserIdAndName> players, String firstPlayerId) {
}
