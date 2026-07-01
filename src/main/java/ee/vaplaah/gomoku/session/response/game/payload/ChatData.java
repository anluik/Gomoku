package ee.vaplaah.gomoku.session.response.game.payload;

import ee.vaplaah.gomoku.user.UserIdAndName;

public record ChatData(UserIdAndName sender, String text) {
}
