package ee.vaplaah.tic_tac_toe.handlers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CustomWebSocketHandler implements WebSocketHandler {

    @NonNull
    @Override
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        log.info("[System log] Handling WebSocket Session: {}", session.getId());
        Flux<WebSocketMessage> map = Flux.just("A", "B")
            .map(e -> session.textMessage("Message: " + e));
        return session.send(map);
    }
}
