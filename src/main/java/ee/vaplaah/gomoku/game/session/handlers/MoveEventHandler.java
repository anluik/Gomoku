package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.message.MoveEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import ee.vaplaah.gomoku.utils.GameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Applies a player's move. Turn ownership (derived from move count parity and player order) is the
 * primary concurrency guard: two players can never legitimately move at once. The {@code @Version}
 * on {@link Game} plus the {@link #retryOnConflict()} pipeline additionally covers a move racing
 * another mutation of the same game (e.g. an opponent resign), and {@code moveSeq} rejects stale or
 * duplicated deliveries. Guard-save-first: the single version-guarded save happens before the
 * {@code GameResult} insert and any broadcast.
 */
@Component
@RequiredArgsConstructor
public class MoveEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;
    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.MOVE;
    }

    @Override
    public boolean retryOnConflict() {
        return true;
    }

    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        MoveEvent moveEvent = (MoveEvent) event;

        Mono<SessionResponse<?>> rejection = validate(game, user, moveEvent);
        if (rejection != null) {
            return rejection;
        }

        Move placed = new Move(moveEvent.getX(), moveEvent.getY(), user.getId(), System.currentTimeMillis());
        game.getMoves().add(placed);
        game.setLastPlayer(user.getId());
        boolean won = GameUtils.isWinningMove(game.getMoves(), game.getWinningCount(), placed);
        if (won) {
            game.setOver(true);
        }

        return gameRepository.save(game)
            .flatMap(saved -> {
                gameSessionManager.broadcast(saved.getId(),
                    GameResponses.moveMade(saved.getId(), placed, saved.getMoves().size()));
                if (won) {
                    return saveWinResult(saved, user.getId())
                        .doOnNext(result -> gameSessionManager.broadcast(saved.getId(),
                            GameResponses.gameWon(saved.getId(), user.getId())))
                        .then(Mono.<SessionResponse<?>>empty());
                }
                return Mono.<SessionResponse<?>>empty();
            });
    }

    /**
     * @return an error response Mono if the move is illegal, or {@code null} if it may be applied.
     */
    private Mono<SessionResponse<?>> validate(Game game, User user, MoveEvent move) {
        String gameId = game.getId();

        if (game.getPlayers().size() < JoinGameEventHandler.MAX_PLAYERS) {
            return Mono.error(new SessionMessageProcessingException(GameResponses.gameNotReady(gameId)));
        }

        UserIdAndName expected = game.getPlayers().get(game.getMoves().size() % JoinGameEventHandler.MAX_PLAYERS);
        if (!expected.getUserId().equals(user.getId())) {
            return Mono.error(new SessionMessageProcessingException(GameResponses.notYourTurn(gameId)));
        }

        if (move.getMoveSeq() != game.getMoves().size()) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.invalidMove(gameId, "Stale or duplicate move sequence")));
        }

        int boardSize = game.getBoardSize();
        if (move.getX() < 0 || move.getX() >= boardSize || move.getY() < 0 || move.getY() >= boardSize) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.invalidMove(gameId, "Move is out of bounds")));
        }

        boolean occupied = game.getMoves().stream()
            .anyMatch(existing -> existing.x() == move.getX() && existing.y() == move.getY());
        if (occupied) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.invalidMove(gameId, "Cell is already occupied")));
        }

        return null;
    }

    private Mono<GameResult> saveWinResult(Game game, String winnerId) {
        return gameResultRepository.save(GameResult.builder()
            .gameId(game.getId())
            .winnerId(winnerId)
            .players(game.getPlayers())
            .resultType(GameResult.ResultType.WIN)
            .movesCount(game.getMoves().size())
            .build());
    }
}
