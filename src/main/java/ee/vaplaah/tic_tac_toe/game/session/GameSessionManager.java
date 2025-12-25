package ee.vaplaah.tic_tac_toe.game.session;

import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {
    // Map of game ID -> Sink
    private final Map<String, Sinks.Many<BaseSessionResponse<?>>> gameSinks = new ConcurrentHashMap<>();

    public Flux<BaseSessionResponse<?>> getGameStream(String gameId) {
        return getOrCreateSink(gameId).asFlux();
    }

    public void broadcast(String gameId, BaseSessionResponse<?> message) {
        getOrCreateSink(gameId).tryEmitNext(message);
    }

    private Sinks.Many<BaseSessionResponse<?>> getOrCreateSink(String gameId) {
        return gameSinks.computeIfAbsent(gameId, id ->
            Sinks.many().multicast().directBestEffort());
    }
}
