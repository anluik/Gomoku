package ee.vaplaah.tic_tac_toe.session.handlers;

import ee.vaplaah.tic_tac_toe.session.SessionMessageProcessor;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionHandler implements WebSocketHandler {

    private final SessionMessageProcessor messageProcessor;

    @NonNull
    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        log.info("[GameSessionHandler] WebSocket connection established to game {}", session.getId()); // TODO: include user ID in the message

        return session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(textPayload -> messageProcessor.process(session, textPayload, GameEvent.class))
            .doOnNext(this::handleValidGameEvent)
            .then();
    }

    private void handleValidGameEvent(GameEvent event) {
        log.info("[GameSessionHandler] Received valid GameEvent message: {}", event);
        // Your successful business logic here
    }
}
