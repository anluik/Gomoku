package ee.vaplaah.gomoku.game.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of who is currently connected to each game — as opposed to who is actually part of
 * the game, which is stored permanently in {@code Game.players}.
 *
 * <p>A single user can have several connections open to the same game at once (for example, the
 * game open in two browser tabs). To handle this, we keep a count of how many open connections
 * each user has per game, and only two moments actually matter:
 * <ul>
 *   <li>the count goes from 0 to 1 — the user just <em>arrived</em>;</li>
 *   <li>the count goes from 1 to 0 — the user's <em>last</em> connection closed, so they have now
 *       truly left the screen (this is what may start an abandonment timer).</li>
 * </ul>
 * Closing one of several tabs, or a quick refresh, does not count as leaving because at least one
 * connection is still open.
 *
 * <p>This state is held in memory on a single server (MVP). Running the app across multiple servers
 * would require storing it somewhere shared — see the game-session architecture rule.
 */
@Slf4j
@Service
public class GamePresenceTracker {

    private final Map<String, Map<String, Integer>> presenceByGame = new ConcurrentHashMap<>();

    /**
     * @return {@code true} if this is the user's first live connection for the game (0 -> 1).
     */
    public boolean connect(String gameId, String userId) {
        boolean[] first = {false};
        int[] connectionCount = {0};
        presenceByGame.compute(gameId, (gid, users) -> {
            Map<String, Integer> counts = users == null ? new ConcurrentHashMap<>() : users;
            counts.merge(userId, 1, Integer::sum);
            connectionCount[0] = counts.get(userId);
            first[0] = connectionCount[0] == 1;
            return counts;
        });
        log.info("PRESENCE connect: game={} user={} connections={} firstConnection={}",
            gameId, userId, connectionCount[0], first[0]);
        return first[0];
    }

    /**
     * @return {@code true} if this was the user's last live connection for the game (1 -> 0).
     */
    public boolean disconnect(String gameId, String userId) {
        boolean[] last = {false};
        int[] remaining = {0};
        presenceByGame.computeIfPresent(gameId, (gid, counts) -> {
            counts.computeIfPresent(userId, (uid, count) -> {
                if (count <= 1) {
                    last[0] = true;
                    return null;
                }
                remaining[0] = count - 1;
                return count - 1;
            });
            return counts.isEmpty() ? null : counts;
        });
        log.info("PRESENCE disconnect: game={} user={} remainingConnections={} lastConnection={}",
            gameId, userId, remaining[0], last[0]);
        return last[0];
    }

    public boolean isPresent(String gameId, String userId) {
        Map<String, Integer> counts = presenceByGame.get(gameId);
        return counts != null && counts.containsKey(userId);
    }

    /**
     * @return {@code true} if no user has any live connection to the game.
     */
    public boolean isGameEmpty(String gameId) {
        Map<String, Integer> counts = presenceByGame.get(gameId);
        return counts == null || counts.isEmpty();
    }
}
