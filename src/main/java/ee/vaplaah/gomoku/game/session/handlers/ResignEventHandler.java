package ee.vaplaah.gomoku.game.session.handlers;

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
import ee.vaplaah.gomoku.user.UserIdAndName;
import ee.vaplaah.gomoku.utils.GameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ResignEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;
    private final GameResultRepository gameResultRepository;
    private final GameRepository gameRepository;

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.RESIGN;
    }

    @Override
    public boolean retryOnConflict() {
        return true;
    }

    @Transactional
    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        return completeGame(game)
            .flatMap(savedGame -> saveGameResult(savedGame, user.getId())
                .then(broadcastUserResigned(savedGame, user)));
    }

    private Mono<GameResult> saveGameResult(Game game, String userId) {
        UserIdAndName otherPlayer = GameUtils.getOtherPlayer(game.getPlayers(), userId);
        GameResult result = GameResult.builder()
            .gameId(game.getId())
            .winnerId(otherPlayer.getUserId())
            .players(game.getPlayers())
            .resultType(GameResult.ResultType.RESIGN)
            .movesCount(game.getMoves().size())
            .build();
        return gameResultRepository.save(result);
    }

    private Mono<Game> completeGame(Game game) {
        game.setOver(true);
        return this.gameRepository.save(game);
    }

    private Mono<SessionResponse<?>> broadcastUserResigned(Game game, User user) {
        UserIdAndName winner = GameUtils.getOtherPlayer(game.getPlayers(), user.getId());
        SessionResponse<?> broadcast = GameResponses.userResigned(
            game.getId(), UserIdAndName.fromUser(user), winner.getUserId(), true);
        gameSessionManager.broadcast(game.getId(), broadcast);
        return Mono.empty();
    }
}
