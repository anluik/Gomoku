package ee.vaplaah.gomoku.session.response.game.payload;

import ee.vaplaah.gomoku.user.UserIdAndName;

public record UserResignedData(UserIdAndName player, String winnerId, boolean over) {
}
