package ee.vaplaah.gomoku.game.session;

import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.game.session.handlers.JoinGameEventHandler;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.UserIdAndName;
import ee.vaplaah.gomoku.utils.GameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles player abandonment: when a player's last connection drops during an active game, the
 * opponent is credited only if the player fails to reconnect within a grace period.
 *
 * <p>Outcome depends on how far the game got: until <em>both</em> players have made at least one
 * move the game is aborted ({@code ABORT}, no winner); once both have moved, the abandoning
 * player's opponent wins ({@code WIN}). If the opponent is also absent when the timer fires, the
 * game is aborted instead.
 *
 * <p>Timers live in-memory and are single-server (MVP): a restart mid-grace strands the game. This
 * moves behind the game's owning node when the app scales out &mdash; see the game-session rule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonmentService {

    private static final long MAX_CONFLICT_RETRIES = 3;

    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;
    private final GameSessionManager gameSessionManager;
    private final GamePresenceTracker presenceTracker;

    @Value("${game.abandonment.grace-seconds:30}")
    private long graceSeconds;

    private final Map<String, ForfeitTimer> pendingTimers = new ConcurrentHashMap<>();

    private record ForfeitTimer(Disposable disposable, UserIdAndName player) {
    }

    /**
     * Call when a user's last live connection to a game drops. Starts a forfeit timer only if the
     * user is a member of an active game (two players, not over); otherwise a no-op (spectator,
     * pre-start, or finished game).
     */
    public Mono<Void> onPlayerDisconnect(String gameId, String userId) {
        return gameRepository.findById(gameId)
            .flatMap(game -> {
                if (!isActive(game) || !GameUtils.isUserPartOfGame(game.getPlayers(), userId)) {
                    log.info("ABANDON disconnect ignored: game={} user={} (not an active game or not a player)",
                        gameId, userId);
                    return Mono.empty();
                }
                UserIdAndName player = findPlayer(game.getPlayers(), userId);
                log.info("ABANDON disconnect: game={} user={} — starting {}s forfeit timer",
                    gameId, userId, graceSeconds);
                gameSessionManager.broadcast(gameId,
                    GameResponses.playerDisconnected(gameId, player, (int) graceSeconds));
                scheduleForfeit(gameId, userId, player);
                return Mono.empty();
            })
            .then();
    }

    /**
     * Call when a user (re)connects. Cancels any pending forfeit timer and, if one was running,
     * broadcasts that the player is back. Cheap no-op when nothing is pending.
     */
    public void onReconnect(String gameId, String userId) {
        ForfeitTimer pending = pendingTimers.remove(key(gameId, userId));
        if (pending != null) {
            pending.disposable().dispose();
            log.info("ABANDON reconnect: game={} user={} — cancelled pending forfeit timer", gameId, userId);
            gameSessionManager.broadcast(gameId, GameResponses.playerReconnected(gameId, pending.player()));
        }
    }

    private void scheduleForfeit(String gameId, String userId, UserIdAndName player) {
        Disposable timer = Mono.delay(Duration.ofSeconds(graceSeconds))
            .then(Mono.defer(() -> finalizeForfeit(gameId, userId)))
            .subscribe(null,
                err -> log.error("Forfeit finalization failed for game {} user {}", gameId, userId, err));
        ForfeitTimer previous = pendingTimers.put(key(gameId, userId), new ForfeitTimer(timer, player));
        if (previous != null) {
            previous.disposable().dispose();
        }
    }

    private Mono<Void> finalizeForfeit(String gameId, String userId) {
        pendingTimers.remove(key(gameId, userId));
        log.info("ABANDON forfeit timer fired: game={} user={}", gameId, userId);
        if (presenceTracker.isPresent(gameId, userId)) {
            // Reconnected between the timer firing and now.
            log.info("ABANDON forfeit skipped: game={} user={} is present again", gameId, userId);
            return Mono.empty();
        }
        // Guard-save-first: mark the game over (version-guarded) before writing the GameResult or
        // broadcasting, so a lock conflict aborts and retries without a duplicate result. On retry
        // the game reads back as over and short-circuits.
        return gameRepository.findById(gameId)
            .flatMap(game -> {
                if (!isActive(game) || !GameUtils.isUserPartOfGame(game.getPlayers(), userId)) {
                    log.info("ABANDON forfeit skipped: game={} user={} — game no longer active/joinable",
                        gameId, userId);
                    return Mono.<Void>empty();
                }
                game.setOver(true);
                return gameRepository.save(game).flatMap(saved -> completeForfeit(saved, userId));
            })
            .retryWhen(Retry.max(MAX_CONFLICT_RETRIES)
                .filter(OptimisticLockingFailureException.class::isInstance)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
            .then();
    }

    private Mono<Void> completeForfeit(Game game, String absentUserId) {
        UserIdAndName opponent = GameUtils.getOtherPlayer(game.getPlayers(), absentUserId);
        boolean opponentAbsent = !presenceTracker.isPresent(game.getId(), opponent.getUserId());
        boolean bothMoved = bothPlayersHaveMoved(game);

        // A win by abandonment can only be claimed once BOTH players have made at least one move.
        if (!bothMoved || opponentAbsent) {
            log.info("ABANDON result ABORT: game={} absentUser={} bothMoved={} opponentAbsent={}",
                game.getId(), absentUserId, bothMoved, opponentAbsent);
            return saveResult(game, GameResult.ResultType.ABORT, null)
                .doOnNext(r -> gameSessionManager.broadcast(game.getId(), GameResponses.gameAborted(game.getId())))
                .then();
        }
        log.info("ABANDON result WIN: game={} winner={} (opponent of abandoning user {})",
            game.getId(), opponent.getUserId(), absentUserId);
        return saveResult(game, GameResult.ResultType.WIN, opponent.getUserId())
            .doOnNext(r -> gameSessionManager.broadcast(game.getId(),
                GameResponses.gameWon(game.getId(), opponent.getUserId())))
            .then();
    }

    /**
     * @return true only if every player in the game has made at least one move — the point at which
     * the game is considered truly underway and a result can be claimed rather than aborted.
     */
    private boolean bothPlayersHaveMoved(Game game) {
        Set<String> movers = game.getMoves().stream()
            .map(Move::userId)
            .collect(Collectors.toSet());
        return game.getPlayers().stream()
            .allMatch(player -> movers.contains(player.getUserId()));
    }

    private Mono<GameResult> saveResult(Game game, GameResult.ResultType type, String winnerId) {
        return gameResultRepository.save(GameResult.builder()
            .gameId(game.getId())
            .winnerId(winnerId)
            .players(game.getPlayers())
            .resultType(type)
            .movesCount(game.getMoves().size())
            .build());
    }

    private boolean isActive(Game game) {
        return !game.isOver() && game.getPlayers().size() >= JoinGameEventHandler.MAX_PLAYERS;
    }

    private UserIdAndName findPlayer(List<UserIdAndName> players, String userId) {
        return players.stream()
            .filter(player -> player.getUserId().equals(userId))
            .findFirst()
            .orElse(null);
    }

    private String key(String gameId, String userId) {
        return gameId + "::" + userId;
    }
}
