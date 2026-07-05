package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.message.AbortEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbortEventHandler}: abort is the pre-first-move exit (the complement of
 * resign), ends the game with no winner, and also lets a lone creator cancel an un-joined game.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbortEventHandlerTest {

    private static final String GAME_ID = "game-1";
    private static final UserIdAndName PLAYER_A = new UserIdAndName("user-a", "alice");
    private static final UserIdAndName PLAYER_B = new UserIdAndName("user-b", "bob");

    @Mock private GameSessionManager gameSessionManager;
    @Mock private GameResultRepository gameResultRepository;
    @Mock private GameRepository gameRepository;

    private AbortEventHandler handler;
    private Game game;
    private User userA;

    @BeforeEach
    void setUp() {
        handler = new AbortEventHandler(gameSessionManager, gameResultRepository, gameRepository);
        userA = User.builder().id(PLAYER_A.getUserId()).username(PLAYER_A.getUsername()).build();
        game = Game.builder()
            .id(GAME_ID)
            .boardSize(15)
            .winningCount(5)
            .players(new ArrayList<>(List.of(PLAYER_A, PLAYER_B)))
            .moves(new ArrayList<>())
            .build();

        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(gameResultRepository.save(any(GameResult.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    void declaresItsDispatchContract() {
        assertThat(handler.supports(GameEventType.ABORT)).isTrue();
        assertThat(handler.supports(GameEventType.RESIGN)).isFalse();
        assertThat(handler.requiresActiveGame()).isTrue();
        assertThat(handler.requiresParticipant()).isTrue();
        assertThat(handler.retryOnConflict()).isTrue();
    }

    @Test
    void rejectsAbortAfterCallerHasMoved() {
        game.getMoves().add(new Move(0, 0, PLAYER_A.getUserId(), 1L));

        StepVerifier.create(handler.handle(new AbortEvent(), game, userA))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(SessionMessageProcessingException.class);
                SessionResponse<?> response = ((SessionMessageProcessingException) error).getResponse();
                assertThat(response.getResponseEvent()).isEqualTo(SessionResponseEvent.ABORT_NOT_ALLOWED);
                assertThat(response.getGameId()).isEqualTo(GAME_ID);
                assertThat(response.getMessage()).containsIgnoringCase("resign");
            })
            .verify();

        assertThat(game.isOver()).isFalse();
        verifyNoInteractions(gameRepository, gameResultRepository, gameSessionManager);
    }

    @Test
    void abortsGameWithNoWinnerEvenIfOpponentHasMoved() {
        // The opponent moved first; the caller has not moved yet, so abort is still their exit.
        game.getMoves().add(new Move(0, 0, PLAYER_B.getUserId(), 1L));

        StepVerifier.create(handler.handle(new AbortEvent(), game, userA))
            .verifyComplete(); // outcome is communicated via broadcast only

        assertThat(game.isOver()).isTrue();
        verify(gameRepository).save(game);

        GameResult result = capturedResult();
        assertThat(result.getResultType()).isEqualTo(GameResult.ResultType.ABORT);
        assertThat(result.getWinnerId()).isNull();
        assertThat(result.getGameId()).isEqualTo(GAME_ID);
        assertThat(result.getMovesCount()).isEqualTo(1);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<SessionResponse<?>> broadcastCaptor = ArgumentCaptor.forClass((Class) SessionResponse.class);
        verify(gameSessionManager).broadcast(eq(GAME_ID), broadcastCaptor.capture());
        assertThat(broadcastCaptor.getValue().getResponseEvent()).isEqualTo(SessionResponseEvent.GAME_ABORTED);
    }

    @Test
    void loneCreatorCanCancelAnUnjoinedGame() {
        game.setPlayers(new ArrayList<>(List.of(PLAYER_A)));

        StepVerifier.create(handler.handle(new AbortEvent(), game, userA))
            .verifyComplete();

        assertThat(game.isOver()).isTrue();
        GameResult result = capturedResult();
        assertThat(result.getResultType()).isEqualTo(GameResult.ResultType.ABORT);
        assertThat(result.getWinnerId()).isNull();
        assertThat(result.getPlayers()).containsExactly(PLAYER_A);
    }

    @Test
    void guardSaveHappensBeforeResultInsertAndBroadcast() {
        StepVerifier.create(handler.handle(new AbortEvent(), game, userA))
            .verifyComplete();

        InOrder order = inOrder(gameRepository, gameResultRepository, gameSessionManager);
        order.verify(gameRepository).save(game);
        order.verify(gameResultRepository).save(any(GameResult.class));
        order.verify(gameSessionManager).broadcast(eq(GAME_ID), any());
    }

    @Test
    void saveFailurePropagatesWithoutResultOrBroadcast() {
        when(gameRepository.save(any(Game.class))).thenReturn(Mono.error(new RuntimeException("mongo down")));

        StepVerifier.create(handler.handle(new AbortEvent(), game, userA))
            .verifyErrorMessage("mongo down");

        verifyNoInteractions(gameResultRepository, gameSessionManager);
    }

    private GameResult capturedResult() {
        ArgumentCaptor<GameResult> captor = ArgumentCaptor.forClass(GameResult.class);
        verify(gameResultRepository).save(captor.capture());
        return captor.getValue();
    }
}
