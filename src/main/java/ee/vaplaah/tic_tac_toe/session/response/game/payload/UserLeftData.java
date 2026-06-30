package ee.vaplaah.tic_tac_toe.session.response.game.payload;

import ee.vaplaah.tic_tac_toe.user.UserIdAndName;

import java.util.List;

public record UserLeftData(UserIdAndName player, List<UserIdAndName> players, boolean over) {
}
