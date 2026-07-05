package ee.vaplaah.gomoku.session.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.session.AbandonmentService;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GamePresenceTracker;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game.session.handlers.GameEventHandler;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.SessionMessageProcessor;
import ee.vaplaah.gomoku.session.message.ChatEvent;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.response.CommonResponses;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static ee.vaplaah.gomoku.utils.JsonSerializer.JSON_SERIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GameSessionHandler}: the WebSocket session lifecycle (handshake, presence,
 * disconnect cleanup) and the event pipeline (dispatch, precondition gates, error containment,
 * optimistic-lock retry). Collaborators are mocked; the "client" is a faked {@link WebSocketSession}
 * whose sent frames are captured and parsed back from JSON.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameSessionHandlerTest {

    private static final String GAME_ID = "game-1";
    private static final URI GAME_URI = URI.create("ws://localhost:8088/tic_tac_toe?gameId=" + GAME_ID);
    private static final String PAYLOAD = "{\"type\":\"CHAT\",\"text\":\"hi\"}";

    @Mock private SessionMessageProcessor messageProcessor;
    @Mock private GameSessionManager gameSessionManager;
    @Mock private GameRepository gameRepository;
    @Mock private GameResultRepository gameResultRepository;
    @Mock private GamePresenceTracker presenceTracker;
    @Mock private AbandonmentService abandonmentService;
    @Mock private WebSocketSession session;

    private FakeEventHandler eventHandler;
    private GameSessionHandler sessionHandler;

    private User user;
    private Game game;
    private ChatEvent chatEvent;

    /** JSON frames the handler sent to the client, parsed back into trees. */
    private final List<JsonNode> sentFrames = new ArrayList<>();

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").username("alice").build();
        game = Game.builder()
            .id(GAME_ID)
            .boardSize(15)
            .winningCount(5)
            .players(new ArrayList<>(List.of(
                new UserIdAndName("user-1", "alice"),
                new UserIdAndName("user-2", "bob"))))
            .build();
        chatEvent = new ChatEvent();

        eventHandler = new FakeEventHandler(GameEventType.CHAT);
        sessionHandler = new GameSessionHandler(messageProcessor, List.of(eventHandler),
            gameSessionManager, gameRepository, gameResultRepository, presenceTracker, abandonmentService);

        when(session.getId()).thenReturn("session-1");
        when(session.getHandshakeInfo()).thenReturn(handshake(GAME_URI));
        when(session.receive()).thenReturn(Flux.empty());
        when(session.textMessage(anyString())).thenAnswer(inv -> textMessage(inv.getArgument(0)));
        when(session.send(any())).thenAnswer(inv ->
            Flux.from((Publisher<WebSocketMessage>) inv.getArgument(0))
                .map(WebSocketMessage::getPayloadAsText)
                .map(json -> JSON_SERIALIZER.readValue(json, JsonNode.class))
                .doOnNext(sentFrames::add)
                .then());

        when(gameSessionManager.getGameStream(GAME_ID)).thenReturn(Flux.empty());
        when(gameRepository.findById(GAME_ID)).thenReturn(Mono.just(game));
        when(gameResultRepository.findByGameId(GAME_ID)).thenReturn(Mono.empty());
        when(abandonmentService.onPlayerDisconnect(anyString(), anyString())).thenReturn(Mono.empty());
        when(messageProcessor.process(PAYLOAD, GameEvent.class)).thenReturn(Mono.just(chatEvent));
    }

    @Nested
    class Handshake {

        @Test
        void closesConnectionWhenGameIdParameterIsMissing() {
            when(session.getHandshakeInfo()).thenReturn(handshake(URI.create("ws://localhost:8088/tic_tac_toe")));
            when(session.close(any(CloseStatus.class))).thenReturn(Mono.empty());

            StepVerifier.create(sessionHandler.handle(session)).verifyComplete();

            ArgumentCaptor<CloseStatus> status = ArgumentCaptor.forClass(CloseStatus.class);
            verify(session).close(status.capture());
            assertThat(status.getValue().getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
            assertThat(status.getValue().getReason()).contains("gameId");
            verifyNoInteractions(presenceTracker, abandonmentService, gameSessionManager);
        }

        @Test
        void closesConnectionWhenGameIdParameterIsBlank() {
            when(session.getHandshakeInfo()).thenReturn(handshake(URI.create("ws://localhost:8088/tic_tac_toe?gameId=")));
            when(session.close(any(CloseStatus.class))).thenReturn(Mono.empty());

            StepVerifier.create(sessionHandler.handle(session)).verifyComplete();

            verify(session).close(any(CloseStatus.class));
            verifyNoInteractions(presenceTracker, abandonmentService, gameSessionManager);
        }

        @Test
        void registersPresenceAndCancelsPendingForfeitOnConnect() {
            runSession();

            verify(presenceTracker).connect(GAME_ID, "user-1");
            verify(abandonmentService).onReconnect(GAME_ID, "user-1");
        }

        @Test
        void completesWithoutSideEffectsWhenUnauthenticated() {
            // No security context on the subscription.
            StepVerifier.create(sessionHandler.handle(session)).verifyComplete();

            verifyNoInteractions(presenceTracker, abandonmentService);
        }
    }

    @Nested
    class EventDispatch {

        @Test
        void routesEventToSupportingHandlerAndSendsItsResponse() {
            runSession(PAYLOAD);

            assertThat(eventHandler.invocations.get()).isEqualTo(1);
            assertThat(eventHandler.lastEvent).isSameAs(chatEvent);
            assertThat(eventHandler.lastGame).isSameAs(game);
            assertThat(eventHandler.lastUser).isSameAs(user);
            assertSingleFrame(SessionResponseEvent.CHAT_MESSAGE, "SUCCESS");
        }

        @Test
        void rejectsEventTypeNoHandlerSupports() {
            GameEvent moveEvent = new GameEvent(GameEventType.MOVE) { };
            when(messageProcessor.process(PAYLOAD, GameEvent.class)).thenReturn(Mono.just(moveEvent));

            runSession(PAYLOAD);

            assertSingleFrame(SessionResponseEvent.UNSUPPORTED_EVENT, "ERROR");
            assertThat(eventHandler.invocations.get()).isZero();
            verify(gameRepository, never()).findById(anyString());
        }

        @Test
        void sendsProcessingErrorAndKeepsSessionAlive() {
            when(messageProcessor.process("garbage", GameEvent.class)).thenReturn(Mono.error(
                new SessionMessageProcessingException(CommonResponses.malformedPayload("bad json"))));

            runSession("garbage", PAYLOAD);

            assertThat(sentFrames).hasSize(2);
            assertThat(frameEvent(0)).isEqualTo(SessionResponseEvent.MALFORMED_PAYLOAD.name());
            assertThat(frameEvent(1)).isEqualTo(SessionResponseEvent.CHAT_MESSAGE.name());
        }

        @Test
        void sendsUnexpectedErrorResponseAndKeepsSessionAlive() {
            eventHandler.results = attempt -> attempt == 1
                ? Mono.error(new IllegalStateException("boom"))
                : Mono.just(GameResponses.chatMessage(GAME_ID, new UserIdAndName("user-1", "alice"), "hi"));

            runSession(PAYLOAD, PAYLOAD);

            assertThat(sentFrames).hasSize(2);
            assertThat(frameEvent(0)).isEqualTo(SessionResponseEvent.UNEXPECTED_ERROR.name());
            assertThat(frameEvent(1)).isEqualTo(SessionResponseEvent.CHAT_MESSAGE.name());
        }
    }

    @Nested
    class PreconditionGates {

        @Test
        void rejectsEventWhenGameDoesNotExist() {
            when(gameRepository.findById(GAME_ID)).thenReturn(Mono.empty());

            runSession(PAYLOAD);

            assertSingleFrame(SessionResponseEvent.GAME_NOT_FOUND, "ERROR");
            assertThat(eventHandler.invocations.get()).isZero();
        }

        @Test
        void rejectsEventOnFinishedGameWithPersistedWinner() {
            game.setOver(true);
            when(gameResultRepository.findByGameId(GAME_ID)).thenReturn(Mono.just(
                GameResult.builder().gameId(GAME_ID).winnerId("user-2").build()));

            runSession(PAYLOAD);

            assertSingleFrame(SessionResponseEvent.GAME_ALREADY_OVER, "ERROR");
            assertThat(sentFrames.get(0).get("data").get("winnerId").asText()).isEqualTo("user-2");
            assertThat(sentFrames.get(0).get("data").get("over").asBoolean()).isTrue();
            assertThat(eventHandler.invocations.get()).isZero();
        }

        @Test
        void rejectsEventOnFinishedGameWithoutPersistedResult() {
            game.setOver(true);

            runSession(PAYLOAD);

            assertSingleFrame(SessionResponseEvent.GAME_ALREADY_OVER, "ERROR");
            assertThat(sentFrames.get(0).get("data").get("winnerId").isNull()).isTrue();
        }

        @Test
        void allowsEventOnFinishedGameWhenHandlerDoesNotRequireActiveGame() {
            game.setOver(true);
            eventHandler.requiresActiveGame = false;

            runSession(PAYLOAD);

            assertThat(eventHandler.invocations.get()).isEqualTo(1);
            assertSingleFrame(SessionResponseEvent.CHAT_MESSAGE, "SUCCESS");
        }

        @Test
        void rejectsEventFromUserWhoIsNotAParticipant() {
            game.setPlayers(List.of(new UserIdAndName("user-2", "bob"), new UserIdAndName("user-3", "carol")));

            runSession(PAYLOAD);

            assertSingleFrame(SessionResponseEvent.USER_NOT_PART_OF_THE_GAME, "ERROR");
            assertThat(eventHandler.invocations.get()).isZero();
        }

        @Test
        void allowsSpectatorWhenHandlerDoesNotRequireParticipant() {
            game.setPlayers(List.of(new UserIdAndName("user-2", "bob"), new UserIdAndName("user-3", "carol")));
            eventHandler.requiresParticipant = false;

            runSession(PAYLOAD);

            assertThat(eventHandler.invocations.get()).isEqualTo(1);
            assertSingleFrame(SessionResponseEvent.CHAT_MESSAGE, "SUCCESS");
        }
    }

    @Nested
    class Broadcasts {

        @Test
        void forwardsGameBroadcastsToTheClient() {
            when(gameSessionManager.getGameStream(GAME_ID)).thenReturn(Flux.just(
                GameResponses.gameStarted(GAME_ID, game.getPlayers())));

            runSession();

            assertSingleFrame(SessionResponseEvent.GAME_STARTED, "SUCCESS");
        }
    }

    @Nested
    class OptimisticLockRetry {

        @Test
        void retriesOnConflictAndSucceeds() {
            eventHandler.retryOnConflict = true;
            eventHandler.results = attempt -> attempt == 1
                ? Mono.error(new OptimisticLockingFailureException("conflict"))
                : Mono.just(GameResponses.chatMessage(GAME_ID, new UserIdAndName("user-1", "alice"), "hi"));

            runSession(PAYLOAD);

            assertThat(eventHandler.invocations.get()).isEqualTo(2);
            assertSingleFrame(SessionResponseEvent.CHAT_MESSAGE, "SUCCESS");
        }

        @Test
        void givesUpAfterRetriesAreExhausted() {
            eventHandler.retryOnConflict = true;
            eventHandler.results = attempt -> Mono.error(new OptimisticLockingFailureException("conflict"));

            runSession(PAYLOAD);

            // 1 initial attempt + 3 retries, then the original failure surfaces as an unexpected error.
            assertThat(eventHandler.invocations.get()).isEqualTo(4);
            assertSingleUnexpectedErrorFrame();
        }

        @Test
        void doesNotRetryWhenHandlerOptsOut() {
            eventHandler.retryOnConflict = false;
            eventHandler.results = attempt -> Mono.error(new OptimisticLockingFailureException("conflict"));

            runSession(PAYLOAD);

            assertThat(eventHandler.invocations.get()).isEqualTo(1);
            assertSingleUnexpectedErrorFrame();
        }
    }

    @Nested
    class DisconnectCleanup {

        @Test
        void armsAbandonmentAndRemovesSinkWhenLastConnectionOfEmptyGameDrops() {
            when(presenceTracker.disconnect(GAME_ID, "user-1")).thenReturn(true);
            when(presenceTracker.isGameEmpty(GAME_ID)).thenReturn(true);

            runSession();

            verify(abandonmentService).onPlayerDisconnect(GAME_ID, "user-1");
            verify(gameSessionManager).removeSink(GAME_ID);
        }

        @Test
        void keepsSinkAndSkipsAbandonmentWhenOtherConnectionsRemain() {
            when(presenceTracker.disconnect(GAME_ID, "user-1")).thenReturn(false);
            when(presenceTracker.isGameEmpty(GAME_ID)).thenReturn(false);

            runSession();

            verify(abandonmentService, never()).onPlayerDisconnect(anyString(), anyString());
            verify(gameSessionManager, never()).removeSink(anyString());
        }

        @Test
        void containsAbandonmentFailureWithoutBreakingTermination() {
            when(presenceTracker.disconnect(GAME_ID, "user-1")).thenReturn(true);
            when(presenceTracker.isGameEmpty(GAME_ID)).thenReturn(true);
            when(abandonmentService.onPlayerDisconnect(GAME_ID, "user-1"))
                .thenReturn(Mono.error(new IllegalStateException("mongo down")));

            runSession(); // must still complete normally

            verify(gameSessionManager).removeSink(GAME_ID);
        }
    }

    // ========== helpers ==========

    /**
     * Feeds the given incoming payloads through the session as the authenticated user and waits for
     * the connection to terminate; sent frames end up in {@link #sentFrames}.
     */
    private void runSession(String... incomingPayloads) {
        when(session.receive()).thenReturn(Flux.fromArray(incomingPayloads).map(this::textMessage));
        StepVerifier.create(sessionHandler.handle(session)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null))))
            .verifyComplete();
    }

    private void assertSingleFrame(SessionResponseEvent event, String status) {
        assertThat(sentFrames).hasSize(1);
        assertThat(frameEvent(0)).isEqualTo(event.name());
        assertThat(sentFrames.get(0).get("status").asText()).isEqualTo(status);
        assertThat(sentFrames.get(0).get("gameId").asText()).isEqualTo(GAME_ID);
    }

    /** The generic unexpected-error response carries no gameId, unlike game-scoped responses. */
    private void assertSingleUnexpectedErrorFrame() {
        assertThat(sentFrames).hasSize(1);
        assertThat(frameEvent(0)).isEqualTo(SessionResponseEvent.UNEXPECTED_ERROR.name());
        assertThat(sentFrames.get(0).get("status").asText()).isEqualTo("ERROR");
    }

    private String frameEvent(int index) {
        return sentFrames.get(index).get("responseEvent").asText();
    }

    private WebSocketMessage textMessage(String payload) {
        return new WebSocketMessage(WebSocketMessage.Type.TEXT,
            DefaultDataBufferFactory.sharedInstance.wrap(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static HandshakeInfo handshake(URI uri) {
        return new HandshakeInfo(uri, new HttpHeaders(), Mono.empty(), null);
    }

    /**
     * Scriptable {@link GameEventHandler}: flags are plain fields and {@link #results} maps the
     * 1-based invocation number to the outcome, so retry behaviour can be scripted per attempt.
     */
    private static final class FakeEventHandler implements GameEventHandler {

        private final GameEventType supportedType;
        private boolean requiresActiveGame = true;
        private boolean requiresParticipant = true;
        private boolean retryOnConflict = false;

        private final AtomicInteger invocations = new AtomicInteger();
        private GameEvent lastEvent;
        private Game lastGame;
        private User lastUser;

        private IntFunction<Mono<SessionResponse<?>>> results;

        private FakeEventHandler(GameEventType supportedType) {
            this.supportedType = supportedType;
            this.results = attempt -> Mono.just(
                GameResponses.chatMessage(GAME_ID, new UserIdAndName("user-1", "alice"), "hi"));
        }

        @Override
        public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
            lastEvent = event;
            lastGame = game;
            lastUser = user;
            return results.apply(invocations.incrementAndGet());
        }

        @Override
        public boolean supports(GameEventType eventType) {
            return eventType == supportedType;
        }

        @Override
        public boolean requiresActiveGame() {
            return requiresActiveGame;
        }

        @Override
        public boolean requiresParticipant() {
            return requiresParticipant;
        }

        @Override
        public boolean retryOnConflict() {
            return retryOnConflict;
        }
    }
}
