package ee.vaplaah.tic_tac_toe.session.handlers;

import ee.vaplaah.tic_tac_toe.session.SessionMessageProcessor;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionHandler implements WebSocketHandler {

    private final SessionMessageProcessor messageProcessor;

    @NonNull
    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        return SecurityUtils.getUser().flatMap(user -> {
            log.info("WebSocket connection established with id {} for user {}", session.getId(), user.getId());
            return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(textPayload -> messageProcessor.process(session, textPayload, GameEvent.class))
                .doOnNext(event -> handleValidGameEvent(event, user))
                .then();
        });
    }

    private void handleValidGameEvent(GameEvent event, User user) {
        log.info("Handling game event {} for user {}", event, user.getId());
    }
}
