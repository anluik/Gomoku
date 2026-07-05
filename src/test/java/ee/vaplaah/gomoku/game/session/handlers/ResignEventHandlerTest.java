package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.message.ResignEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.session.response.game.payload.UserResignedData;
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
 * Unit tests for {@link ResignEventHandler}: resign is only allowed after the caller's first move
 * (before that, abort is the right exit), credits the opponent with the win, and follows
 * guard-save-first — the version-guarded game save precedes the result insert and the broadcast.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResignEventHandlerTest {

    private static final String GAME_ID = "game-1";
    private static final UserIdAndName PLAYER_A = new UserIdAndName("user-a", "alice");
    private static final UserIdAndName PLAYER_B = new UserIdAndName("user-b", "bob");

    @Mock private GameSessionManager gameSessionManager;
    @Mock private GameResultRepository gameResultRepository;
    @Mock private GameRepository gameRepository;

    private ResignEventHandler handler;
    private Game game;
    private User userA;

    @BeforeEach
    void setUp() {
        handler = new ResignEventHandler(gameSessionManager, gameResultRepository, gameRepository);
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
        assertThat(handler.supports(GameEventType.RESIGN)).isTrue();
        assertThat(handler.supports(GameEventType.ABORT)).isFalse();
        assertThat(handler.requiresActiveGame()).isTrue();
        assertThat(handler.requiresParticipant()).isTrue();
        assertThat(handler.retryOnConflict()).isTrue();
    }

    @Test
    void rejectsResignBeforeCallersFirstMove() {
        // The opponent has moved but the caller has not — the caller must abort, not resign.
        game.getMoves().add(new Move(0, 0, PLAYER_B.getUserId(), 1L));

        StepVerifier.create(handler.handle(new ResignEvent(), game, userA))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(SessionMessageProcessingException.class);
                SessionResponse<?> response = ((SessionMessageProcessingException) error).getResponse();
                assertThat(response.getResponseEvent()).isEqualTo(SessionResponseEvent.RESIGN_NOT_ALLOWED);
                assertThat(response.getGameId()).isEqualTo(GAME_ID);
                assertThat(response.getMessage()).containsIgnoringCase("abort");
            })
            .verify();

        assertThat(game.isOver()).isFalse();
        verifyNoInteractions(gameRepository, gameResultRepository, gameSessionManager);
    }

    @Test
    void resignAfterMovingEndsGameAndCreditsOpponent() {
        game.getMoves().add(new Move(0, 0, PLAYER_A.getUserId(), 1L));
        game.getMoves().add(new Move(1, 1, PLAYER_B.getUserId(), 2L));

        StepVerifier.create(handler.handle(new ResignEvent(), game, userA))
            .verifyComplete(); // outcome is communicated via broadcast only

        assertThat(game.isOver()).isTrue();
        verify(gameRepository).save(game);

        ArgumentCaptor<GameResult> resultCaptor = ArgumentCaptor.forClass(GameResult.class);
        verify(gameResultRepository).save(resultCaptor.capture());
        GameResult result = resultCaptor.getValue();
        assertThat(result.getResultType()).isEqualTo(GameResult.ResultType.RESIGN);
        assertThat(result.getWinnerId()).isEqualTo(PLAYER_B.getUserId());
        assertThat(result.getGameId()).isEqualTo(GAME_ID);
        assertThat(result.getMovesCount()).isEqualTo(2);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<SessionResponse<?>> broadcastCaptor = ArgumentCaptor.forClass((Class) SessionResponse.class);
        verify(gameSessionManager).broadcast(eq(GAME_ID), broadcastCaptor.capture());
        SessionResponse<?> broadcast = broadcastCaptor.getValue();
        assertThat(broadcast.getResponseEvent()).isEqualTo(SessionResponseEvent.USER_RESIGNED);
        UserResignedData data = (UserResignedData) broadcast.getData();
        assertThat(data.player().getUserId()).isEqualTo(PLAYER_A.getUserId());
        assertThat(data.winnerId()).isEqualTo(PLAYER_B.getUserId());
        assertThat(data.over()).isTrue();
    }

    @Test
    void guardSaveHappensBeforeResultInsertAndBroadcast() {
        game.getMoves().add(new Move(0, 0, PLAYER_A.getUserId(), 1L));
        game.getMoves().add(new Move(1, 1, PLAYER_B.getUserId(), 2L));

        StepVerifier.create(handler.handle(new ResignEvent(), game, userA))
            .verifyComplete();

        InOrder order = inOrder(gameRepository, gameResultRepository, gameSessionManager);
        order.verify(gameRepository).save(game);
        order.verify(gameResultRepository).save(any(GameResult.class));
        order.verify(gameSessionManager).broadcast(eq(GAME_ID), any());
    }

    @Test
    void saveFailurePropagatesWithoutResultOrBroadcast() {
        game.getMoves().add(new Move(0, 0, PLAYER_A.getUserId(), 1L));
        when(gameRepository.save(any(Game.class))).thenReturn(Mono.error(new RuntimeException("mongo down")));

        StepVerifier.create(handler.handle(new ResignEvent(), game, userA))
            .verifyErrorMessage("mongo down");

        verifyNoInteractions(gameResultRepository, gameSessionManager);
    }
}
