package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.utils.GameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Aborts a game that has not truly started for the caller. A player may abort only until they have
 * made their first move; afterwards they must resign instead (see {@link ResignEventHandler}). An
 * abort ends the game with no winner.
 */
@Component
@RequiredArgsConstructor
public class AbortEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;
    private final GameResultRepository gameResultRepository;
    private final GameRepository gameRepository;

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.ABORT;
    }

    @Override
    public boolean retryOnConflict() {
        return true;
    }

    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        if (GameUtils.hasUserMoved(game.getMoves(), user.getId())) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.abortNotAllowed(game.getId(), "You have already moved; resign instead")));
        }
        return completeGame(game)
            .flatMap(savedGame -> saveAbortResult(savedGame)
                .then(broadcastGameAborted(savedGame)));
    }

    private Mono<Game> completeGame(Game game) {
        game.setOver(true);
        return gameRepository.save(game);
    }

    private Mono<GameResult> saveAbortResult(Game game) {
        return gameResultRepository.save(GameResult.builder()
            .gameId(game.getId())
            .winnerId(null)
            .players(game.getPlayers())
            .resultType(GameResult.ResultType.ABORT)
            .movesCount(game.getMoves().size())
            .build());
    }

    private Mono<SessionResponse<?>> broadcastGameAborted(Game game) {
        gameSessionManager.broadcast(game.getId(), GameResponses.gameAborted(game.getId()));
        return Mono.empty();
    }
}
