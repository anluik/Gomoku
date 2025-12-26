package ee.vaplaah.tic_tac_toe.utils;

import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GameUtils {

    public static UserIdAndName getOtherPlayer(List<UserIdAndName> players, String currentUser) {
        return players.get(0).getUserId().equals(currentUser) ? players.get(1) : players.get(0);
    }

    public static boolean isUserPartOfTheGame(List<UserIdAndName> players, String userId) {
        return players.stream()
            .anyMatch(player -> player.getUserId().equals(userId));
    }
}
