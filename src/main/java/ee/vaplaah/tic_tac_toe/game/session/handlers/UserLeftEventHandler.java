package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.core.exception.SessionMessageProcessingException;
import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.game.GameRepository;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.game.session.GameSessionManager;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponseEvent;
import ee.vaplaah.tic_tac_toe.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserLeftEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;
    private final GameRepository repository;

    @Override
    public Mono<BaseSessionResponse<?>> handle(GameEvent event, User user) {
        String gameId = event.getGameId();
        return repository.findById(gameId)
            .flatMap(game -> {
                if (!game.getPlayers().contains(user.getId())) {
                    BaseSessionResponse<?> response = BaseSessionResponse.builder()
                        .status(ResponseStatus.ERROR)
                        .responseEvent(SessionResponseEvent.INVALID_PAYLOAD)
                        .message("Unable to leave game - user not in game")
                        .build();
                    return Mono.error(new SessionMessageProcessingException(response));
                }
                // TODO: check concurrency?
                game.getPlayers().add(user.getId());
                return repository.save(game);
            })
            .flatMap(savedGame -> {
                var broadcast = BaseSessionResponse.builder()
                    .status(ResponseStatus.SUCCESS)
                    .responseEvent(SessionResponseEvent.USER_LEFT)
                    .message("User " + user.getUsername() + " left the game")
                    .data(savedGame)
                    .build();

                gameSessionManager.broadcast(event.getGameId(), broadcast);

                if (savedGame.getPlayers().isEmpty()) {
                    gameSessionManager.removeSink(event.getGameId());
                }

                return Mono.empty();
            })
            // TODO: game not found?
            ;
    }

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.USER_LEFT;
    }
}
