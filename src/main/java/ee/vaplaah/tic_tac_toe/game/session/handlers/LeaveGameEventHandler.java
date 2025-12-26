package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.game.GameRepository;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.game.session.GameSessionManager;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponseEvent;
import ee.vaplaah.tic_tac_toe.user.User;
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
    public Mono<BaseSessionResponse<?>> handle(GameEvent event, Game game, User user) {
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

    private Mono<BaseSessionResponse<?>> broadcastUserLeftEvent(Game game, User user) {
        var broadcast = BaseSessionResponse.builder()
            .status(ResponseStatus.SUCCESS)
            .responseEvent(SessionResponseEvent.USER_LEFT)
            .message("User " + user.getUsername() + " left the game")
            .data(game)
            .build();

        gameSessionManager.broadcast(game.getId(), broadcast);

        if (game.getPlayers().isEmpty()) {
            // remove sink as no one is listening anymore
            gameSessionManager.removeSink(game.getId());
        }

        return Mono.empty();
    }
}
