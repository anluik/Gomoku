package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.session.message.JoinGameEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.session.response.game.payload.GameStartedData;
import ee.vaplaah.gomoku.session.response.game.payload.UserJoinedData;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JoinGameEventHandler}: seat assignment, the auto-start broadcast when the
 * second player arrives, and the already-joined / game-full rejections.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JoinGameEventHandlerTest {

    private static final String GAME_ID = "game-1";
    private static final UserIdAndName CREATOR = new UserIdAndName("user-a", "alice");

    @Mock private GameSessionManager gameSessionManager;
    @Mock private GameRepository gameRepository;

    private JoinGameEventHandler handler;
    private Game game;
    private User joiner;

    @BeforeEach
    void setUp() {
        handler = new JoinGameEventHandler(gameSessionManager, gameRepository);
        joiner = User.builder().id("user-b").username("bob").build();
        game = Game.builder()
            .id(GAME_ID)
            .boardSize(15)
            .winningCount(5)
            .players(new ArrayList<>(List.of(CREATOR)))
            .build();

        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    void declaresItsDispatchContract() {
        assertThat(handler.supports(GameEventType.JOIN_GAME)).isTrue();
        assertThat(handler.supports(GameEventType.MOVE)).isFalse();
        // Joining is what makes you a participant, so the participant gate must be off.
        assertThat(handler.requiresParticipant()).isFalse();
        assertThat(handler.requiresActiveGame()).isTrue();
        assertThat(handler.retryOnConflict()).isTrue();
    }

    @Test
    void secondPlayerJoiningSeatsThemAndAutoStartsTheGame() {
        StepVerifier.create(handler.handle(new JoinGameEvent(), game, joiner))
            .verifyComplete(); // no direct response; everything goes out via broadcast

        assertThat(game.getPlayers()).extracting(UserIdAndName::getUserId)
            .containsExactly("user-a", "user-b"); // creator keeps seat 0 = first mover
        verify(gameRepository).save(game);

        List<SessionResponse<?>> broadcasts = captureBroadcasts(2);
        assertThat(broadcasts.get(0).getResponseEvent()).isEqualTo(SessionResponseEvent.USER_JOINED);
        UserJoinedData joined = (UserJoinedData) broadcasts.get(0).getData();
        assertThat(joined.player().getUserId()).isEqualTo("user-b");
        assertThat(joined.players()).hasSize(2);

        assertThat(broadcasts.get(1).getResponseEvent()).isEqualTo(SessionResponseEvent.GAME_STARTED);
        GameStartedData started = (GameStartedData) broadcasts.get(1).getData();
        assertThat(started.firstPlayerId()).isEqualTo("user-a");
        assertThat(started.players()).hasSize(2);
    }

    @Test
    void joiningAGameThatIsNotYetFullDoesNotStartIt() {
        game.setPlayers(new ArrayList<>()); // degenerate: no creator seated

        StepVerifier.create(handler.handle(new JoinGameEvent(), game, joiner))
            .verifyComplete();

        List<SessionResponse<?>> broadcasts = captureBroadcasts(1);
        assertThat(broadcasts.get(0).getResponseEvent()).isEqualTo(SessionResponseEvent.USER_JOINED);
    }

    @Test
    void rejectsUserWhoAlreadyHasASeat() {
        User creatorUser = User.builder().id(CREATOR.getUserId()).username(CREATOR.getUsername()).build();

        StepVerifier.create(handler.handle(new JoinGameEvent(), game, creatorUser))
            .expectErrorSatisfies(error -> assertRejection(error, SessionResponseEvent.USER_ALREADY_JOINED))
            .verify();

        assertThat(game.getPlayers()).hasSize(1);
        verifyNoInteractions(gameRepository, gameSessionManager);
    }

    @Test
    void rejectsJoiningAFullGame() {
        game.getPlayers().add(new UserIdAndName("user-b", "bob"));
        User thirdUser = User.builder().id("user-c").username("carol").build();

        StepVerifier.create(handler.handle(new JoinGameEvent(), game, thirdUser))
            .expectErrorSatisfies(error -> assertRejection(error, SessionResponseEvent.GAME_FULL))
            .verify();

        assertThat(game.getPlayers()).hasSize(2);
        verifyNoInteractions(gameRepository, gameSessionManager);
    }

    @Test
    void saveFailurePropagatesWithoutAnyBroadcast() {
        when(gameRepository.save(any(Game.class))).thenReturn(Mono.error(new RuntimeException("mongo down")));

        StepVerifier.create(handler.handle(new JoinGameEvent(), game, joiner))
            .verifyErrorMessage("mongo down");

        verifyNoInteractions(gameSessionManager);
    }

    private void assertRejection(Throwable error, SessionResponseEvent expectedEvent) {
        assertThat(error).isInstanceOf(SessionMessageProcessingException.class);
        SessionResponse<?> response = ((SessionMessageProcessingException) error).getResponse();
        assertThat(response.getResponseEvent()).isEqualTo(expectedEvent);
        assertThat(response.getGameId()).isEqualTo(GAME_ID);
        assertThat(response.getData()).isEqualTo(game.getPlayers()); // rejection carries the seat list
    }

    private List<SessionResponse<?>> captureBroadcasts(int expectedCount) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<SessionResponse<?>> captor = ArgumentCaptor.forClass((Class) SessionResponse.class);
        verify(gameSessionManager, times(expectedCount)).broadcast(eq(GAME_ID), captor.capture());
        return captor.getAllValues();
    }
}
