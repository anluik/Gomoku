package ee.vaplaah.gomoku.session.response.game.payload;

import ee.vaplaah.gomoku.user.UserIdAndName;

/**
 * Live-connection presence change for a player. {@code connected=false} carries the
 * {@code graceSeconds} the player has to reconnect before the game is forfeited; on reconnect
 * {@code connected=true} and {@code graceSeconds} is null.
 */
public record PresenceData(UserIdAndName player, boolean connected, Integer graceSeconds) {
}
