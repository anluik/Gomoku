package ee.vaplaah.tic_tac_toe.session.response.game.payload;

import ee.vaplaah.tic_tac_toe.user.UserIdAndName;

public record UserResignedData(UserIdAndName player, String winnerId, boolean over) {
}
