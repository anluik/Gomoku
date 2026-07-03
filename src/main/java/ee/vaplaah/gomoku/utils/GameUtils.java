package ee.vaplaah.gomoku.utils;

import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.user.UserIdAndName;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class GameUtils {

    private static final int[][] LINE_DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

    public static UserIdAndName getOtherPlayer(List<UserIdAndName> players, String currentUser) {
        return players.get(0).getUserId().equals(currentUser) ? players.get(1) : players.get(0);
    }

    public static boolean isUserPartOfGame(List<UserIdAndName> players, String userId) {
        return players.stream()
            .anyMatch(player -> player.getUserId().equals(userId));
    }

    public static boolean hasUserMoved(List<Move> moves, String userId) {
        return moves.stream()
            .anyMatch(move -> move.userId().equals(userId));
    }

    /**
     * Returns true if {@code last} completes a line of at least {@code winningCount} cells owned by
     * the same player. Only the four axes through the last move are scanned (horizontal, vertical,
     * both diagonals), counting outward in both directions.
     */
    public static boolean isWinningMove(List<Move> moves, int winningCount, Move last) {
        Map<String, String> owners = new HashMap<>();
        for (Move move : moves) {
            owners.put(cellKey(move.x(), move.y()), move.userId());
        }
        for (int[] dir : LINE_DIRECTIONS) {
            int inARow = 1
                + countConsecutive(owners, last, dir[0], dir[1])
                + countConsecutive(owners, last, -dir[0], -dir[1]);
            if (inARow >= winningCount) {
                return true;
            }
        }
        return false;
    }

    private static int countConsecutive(Map<String, String> owners, Move from, int dx, int dy) {
        String owner = from.userId();
        int count = 0;
        int x = from.x() + dx;
        int y = from.y() + dy;
        while (owner.equals(owners.get(cellKey(x, y)))) {
            count++;
            x += dx;
            y += dy;
        }
        return count;
    }

    private static String cellKey(int x, int y) {
        return x + "," + y;
    }
}
