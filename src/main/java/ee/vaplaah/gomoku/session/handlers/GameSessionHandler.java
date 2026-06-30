package ee.vaplaah.gomoku.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game.session.handlers.GameEventHandler;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.SessionMessageProcessor;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.response.CommonResponses;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.game.payload.GameOutcomeData;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.utils.SecurityUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static ee.vaplaah.gomoku.utils.GameUtils.isUserPartOfGame;
import static ee.vaplaah.gomoku.utils.JsonSerializer.JSON_SERIALIZER;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionHandler implements WebSocketHandler {

    private final SessionMessageProcessor messageProcessor;
    private final List<GameEventHandler> gameEventHandlers;
    private final GameSessionManager gameSessionManager;
    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;

    @NonNull
    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        String gameId = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
            .build().getQueryParams().getFirst("gameId");
        if (gameId == null || gameId.isBlank()) {
            return session.close(CloseStatus.BAD_DATA.withReason("Parameter 'gameId' is required"));
        }

        return SecurityUtils.getUser().flatMap(user -> {
            log.info("WebSocket connection established. Session: {}, User: {}, Game: {}.",
                session.getId(), user.getId(), gameId);

            // Action stream - messages from current user
            Flux<SessionResponse<?>> actions = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .concatMap(payload ->
                    // process and validate incoming message
                    messageProcessor.process(payload, GameEvent.class)
                        .flatMap(event -> handleValidGameEvent(gameId, event, user))
                        // catch errors from processing or handling
                        .onErrorResume(SessionMessageProcessingException.class, e -> Mono.just(e.getResponse()))
                        // catch any unexpected error from processing or handlers
                        .onErrorResume(e -> {
                            log.error("Unexpected error occurred", e);
                            return Mono.just(CommonResponses.unexpected());
                        })
                );

            // Broadcast stream - messages from other users in the same game
            Flux<SessionResponse<?>> broadcasts = gameSessionManager.getGameStream(gameId);

            // TODO: should handle a JOIN_GAME event here? How would it work with spectator mode?
            return session.send(
                Flux.merge(broadcasts, actions)
                    .map(JSON_SERIALIZER::writeAsJson)
                    .map(session::textMessage)
            ).doFinally(signalType -> {
                log.info("WebSocket connection terminated with signal {}. Session: {}, User: {}, Game: {}.",
                    signalType, session.getId(), user.getId(), gameId);
                handleValidGameEvent(gameId, new GameEvent(GameEventType.LEAVE_GAME), user).subscribe();
            });
        });
    }

    private Mono<SessionResponse<?>> handleValidGameEvent(String gameId, GameEvent event, User user) {
        log.info("Handling game event {} for user {}", event.getType(), user.getId());
        GameEventHandler handler = gameEventHandlers.stream()
            .filter(h -> h.supports(event.getType()))
            .findFirst()
            .orElse(null);

        if (handler == null) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.unsupportedEvent(gameId, event.getType())));
        }

        // TODO: implement games cache
        return gameRepository.findById(gameId)
            .flatMap(game -> {
                // TODO: what about if the game has started?
                if (handler.requiresActiveGame() && game.isOver()) {
                    return gameAlreadyOverError(game.getId());
                }
                if (handler.requiresParticipant() && !isUserPartOfGame(game.getPlayers(), user.getId())) {
                    return Mono.error(new SessionMessageProcessingException(
                        GameResponses.userNotPartOfTheGame(game.getId())));
                }
                return handler.handle(event, game, user);
            })
            .switchIfEmpty(Mono.error(new SessionMessageProcessingException(
                GameResponses.gameNotFound(gameId))));
    }

    private Mono<SessionResponse<?>> gameAlreadyOverError(String gameId) {
        // The winner is not stored on Game, so look up the GameResult. A game can also be
        // marked over with no result (e.g. all players left), hence the default outcome.
        return gameResultRepository.findByGameId(gameId)
            .map(result -> new GameOutcomeData(result.getWinnerId(), true))
            .defaultIfEmpty(new GameOutcomeData(null, true))
            .flatMap(outcome -> Mono.error(new SessionMessageProcessingException(
                GameResponses.gameAlreadyOver(gameId, outcome))));
    }
}
