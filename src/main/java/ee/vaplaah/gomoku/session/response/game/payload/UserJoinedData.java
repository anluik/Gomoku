package ee.vaplaah.gomoku.session.response.game.payload;

import ee.vaplaah.gomoku.user.UserIdAndName;

import java.util.List;

public record UserJoinedData(UserIdAndName player, List<UserIdAndName> players) {
}
