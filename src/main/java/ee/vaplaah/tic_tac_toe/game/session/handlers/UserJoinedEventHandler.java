package ee.vaplaah.tic_tac_toe.game.session.handlers;

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
public class UserJoinedEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;
    private final GameRepository repository;

    @Override
    public Mono<BaseSessionResponse<?>> handle(GameEvent event, User user) {
        String gameId = event.getGameId();
        return repository.findById(gameId)
            .flatMap(game -> {
                // Check if game is full
                if (game.getPlayers().size() >= 2) {
                    return Mono.error(new IllegalStateException("Game is full"));
                }
                // TODO: check concurrency?
                game.getPlayers().add(user.getId());
                return repository.save(game);
            })
            .flatMap(savedGame -> {
                var broadcastMessage = BaseSessionResponse.builder()
                    .status(ResponseStatus.SUCCESS)
                    .responseEvent(SessionResponseEvent.USER_JOINED)
                    .data(savedGame.getPlayers()) // Send updated list to everyone
//                    .correlationId(event.getCorrelationId()) // Important for the sender!
                    .build();

                // Broadcast to all subscribers of this game
                gameSessionManager.broadcast(gameId, broadcastMessage);

                // Empty direct response to websocket message. Update is communicated via broadcast.
                return Mono.empty();
            })
            // TODO: game not found?
            ;
    }

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.USER_JOINED;
    }
}
