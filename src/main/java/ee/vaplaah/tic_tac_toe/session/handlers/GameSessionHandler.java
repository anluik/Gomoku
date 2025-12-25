package ee.vaplaah.tic_tac_toe.session.handlers;

import ee.vaplaah.tic_tac_toe.core.exception.SessionMessageProcessingException;
import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.game.session.handlers.GameEventHandler;
import ee.vaplaah.tic_tac_toe.session.SessionMessageProcessor;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponseEvent;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.utils.SecurityUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.List;

import static ee.vaplaah.tic_tac_toe.utils.JsonSerializer.JSON_SERIALIZER;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionHandler implements WebSocketHandler {

    private final SessionMessageProcessor messageProcessor;
    private final List<GameEventHandler> gameEventHandlers;

    @NonNull
    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        return SecurityUtils.getUser().flatMap(user -> {
            log.info("WebSocket connection established with session id {} with user {}", session.getId(), user.getId());
            return session.send(
                session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .concatMap(payload ->
                        // process and validate incoming message
                        messageProcessor.process(payload, GameEvent.class)
                            .flatMap(event -> handleValidGameEvent(event, user))
                            // catch message processing error from messageProcessor
                            .onErrorResume(SessionMessageProcessingException.class, e -> Mono.just(e.getResponse()))
                            // catch any unexpected error from processing or handlers
                            .onErrorResume(e -> Mono.just(BaseSessionResponse.builder()
                                .responseEvent(SessionResponseEvent.UNEXPECTED_ERROR)
                                .message("Unexpected Server Error")
                                .status(ResponseStatus.ERROR)
                                .build()))
                    )
                    .map(JSON_SERIALIZER::writeAsJson)
                    .map(session::textMessage)
            );
        });
    }

    private Mono<BaseSessionResponse<?>> handleValidGameEvent(GameEvent event, User user) {
        log.info("Handling game event {} for user {}", event, user.getId());
        return gameEventHandlers.stream()
            .filter(handler -> handler.supports(event.getType()))
            .findFirst()
            .map(handler -> handler.handle(event, user))
            .orElseGet(() -> {
                BaseSessionResponse<?> baseSessionResponse = BaseSessionResponse.builder()
                    .status(ResponseStatus.ERROR)
                    .responseEvent(SessionResponseEvent.INVALID_PAYLOAD)
                    .message("Unsupported event type: " + event.getType())
                    .build();
                return Mono.just(baseSessionResponse);
            });
    }
}
