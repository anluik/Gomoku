package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.game.GameRepository;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.game.session.GameSessionManager;
import ee.vaplaah.tic_tac_toe.game_result.GameResult;
import ee.vaplaah.tic_tac_toe.game_result.GameResultRepository;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.game.GameResponses;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import ee.vaplaah.tic_tac_toe.utils.GameUtils;
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

    @Transactional
    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        return saveGameResult(game, user.getId())
            .flatMap(result -> completeGame(game))
            .flatMap(savedGame -> broadcastUserResigned(savedGame, user));
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
