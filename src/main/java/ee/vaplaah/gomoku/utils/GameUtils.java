package ee.vaplaah.gomoku.utils;

import ee.vaplaah.gomoku.user.UserIdAndName;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GameUtils {

    public static UserIdAndName getOtherPlayer(List<UserIdAndName> players, String currentUser) {
        return players.get(0).getUserId().equals(currentUser) ? players.get(1) : players.get(0);
    }

    public static boolean isUserPartOfGame(List<UserIdAndName> players, String userId) {
        return players.stream()
            .anyMatch(player -> player.getUserId().equals(userId));
    }
}
