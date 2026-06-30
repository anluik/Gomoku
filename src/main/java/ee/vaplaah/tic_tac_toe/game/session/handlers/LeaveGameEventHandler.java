package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.game.GameRepository;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.game.session.GameSessionManager;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.game.GameResponses;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LeaveGameEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;
    private final GameRepository repository;

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.LEAVE_GAME;
    }

    @Transactional
    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        return removeUserFromTheGame(game, user.getId())
            .flatMap(savedGame -> broadcastUserLeftEvent(savedGame, user));
    }

    private Mono<Game> removeUserFromTheGame(Game game, String userId) {
        game.getPlayers().removeIf(player -> player.getUserId().equals(userId));
        if (game.getPlayers().isEmpty()) {
            // don't keep empty games - users should create a new one instead of rejoining old
            game.setOver(true);
        }
        return repository.save(game);
    }

    private Mono<SessionResponse<?>> broadcastUserLeftEvent(Game game, User user) {
        SessionResponse<?> broadcast = GameResponses.userLeft(
            game.getId(), UserIdAndName.fromUser(user), game.getPlayers(), game.isOver());

        gameSessionManager.broadcast(game.getId(), broadcast);

        if (game.getPlayers().isEmpty()) {
            // remove sink as no one is listening anymore
            gameSessionManager.removeSink(game.getId());
        }

        return Mono.empty();
    }
}
