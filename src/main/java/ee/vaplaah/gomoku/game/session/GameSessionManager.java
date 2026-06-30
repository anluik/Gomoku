package ee.vaplaah.gomoku.game.session;

import ee.vaplaah.gomoku.session.response.SessionResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {
    // Map of game ID -> Sink
    private final Map<String, Sinks.Many<SessionResponse<?>>> gameSinks = new ConcurrentHashMap<>();

    public Flux<SessionResponse<?>> getGameStream(String gameId) {
        return getOrCreateSink(gameId).asFlux();
    }

    public void broadcast(String gameId, SessionResponse<?> message) {
        getOrCreateSink(gameId).tryEmitNext(message);
    }

    private Sinks.Many<SessionResponse<?>> getOrCreateSink(String gameId) {
        return gameSinks.computeIfAbsent(gameId, id ->
            Sinks.many().multicast().directBestEffort());
    }

    public void removeSink(String gameId) {
        Sinks.Many<?> sink = gameSinks.remove(gameId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
