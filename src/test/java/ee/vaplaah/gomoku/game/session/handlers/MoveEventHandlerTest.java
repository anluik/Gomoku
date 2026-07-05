package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.game_result.GameResultRepository;
import ee.vaplaah.gomoku.session.message.MoveEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.session.response.game.payload.MoveData;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import static ee.vaplaah.gomoku.utils.JsonSerializer.JSON_SERIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MoveEventHandler}: move validation (readiness, turn order, sequence number,
 * bounds, occupancy), state mutation, and the terminal outcomes (win / draw) including the
 * guard-save-first ordering of save, result insert, and broadcasts.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MoveEventHandlerTest {

    private static final String GAME_ID = "game-1";
    private static final UserIdAndName PLAYER_A = new UserIdAndName("user-a", "alice");
    private static final UserIdAndName PLAYER_B = new UserIdAndName("user-b", "bob");

    @Mock private GameSessionManager gameSessionManager;
    @Mock private GameRepository gameRepository;
    @Mock private GameResultRepository gameResultRepository;

    private MoveEventHandler handler;
    private Game game;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        handler = new MoveEventHandler(gameSessionManager, gameRepository, gameResultRepository);
        userA = User.builder().id(PLAYER_A.getUserId()).username(PLAYER_A.getUsername()).build();
        userB = User.builder().id(PLAYER_B.getUserId()).username(PLAYER_B.getUsername()).build();
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
    void supportsOnlyMoveEvents() {
        assertThat(handler.supports(GameEventType.MOVE)).isTrue();
        assertThat(handler.supports(GameEventType.CHAT)).isFalse();
    }

    @Test
    void optsIntoConflictRetryBecauseItFollowsGuardSaveFirst() {
        assertThat(handler.retryOnConflict()).isTrue();
    }

    @Nested
    class Validation {

        @Test
        void rejectsMoveBeforeBothPlayersJoined() {
            game.setPlayers(new ArrayList<>(List.of(PLAYER_A)));

            expectRejection(moveEvent(0, 0, 0), userA, SessionResponseEvent.GAME_NOT_READY, null);
        }

        @Test
        void rejectsSecondPlayerMovingFirst() {
            expectRejection(moveEvent(0, 0, 0), userB, SessionResponseEvent.NOT_YOUR_TURN, null);
        }

        @Test
        void rejectsPlayerMovingTwiceInARow() {
            game.getMoves().add(new Move(0, 0, PLAYER_A.getUserId(), 1L));

            expectRejection(moveEvent(0, 1, 1), userA, SessionResponseEvent.NOT_YOUR_TURN, null);
        }

        @Test
        void rejectsStaleOrDuplicateMoveSequence() {
            game.getMoves().add(new Move(0, 0, PLAYER_A.getUserId(), 1L));

            // It is B's turn and move count is 1, but the event replays sequence 0.
            expectRejection(moveEvent(3, 3, 0), userB, SessionResponseEvent.INVALID_MOVE, "Stale or duplicate");
        }

        @Test
        void rejectsOutOfBoundsMoves() {
            expectRejection(moveEvent(-1, 0, 0), userA, SessionResponseEvent.INVALID_MOVE, "out of bounds");
            expectRejection(moveEvent(0, -1, 0), userA, SessionResponseEvent.INVALID_MOVE, "out of bounds");
            expectRejection(moveEvent(15, 0, 0), userA, SessionResponseEvent.INVALID_MOVE, "out of bounds");
            expectRejection(moveEvent(0, 15, 0), userA, SessionResponseEvent.INVALID_MOVE, "out of bounds");
        }

        @Test
        void rejectsMoveOntoOccupiedCell() {
            game.getMoves().add(new Move(7, 7, PLAYER_A.getUserId(), 1L));

            expectRejection(moveEvent(7, 7, 1), userB, SessionResponseEvent.INVALID_MOVE, "occupied");
        }

        private void expectRejection(MoveEvent event, User mover, SessionResponseEvent expectedEvent,
                                     String messageFragment) {
            int movesBefore = game.getMoves().size();

            StepVerifier.create(handler.handle(event, game, mover))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SessionMessageProcessingException.class);
                    SessionResponse<?> response = ((SessionMessageProcessingException) error).getResponse();
                    assertThat(response.getResponseEvent()).isEqualTo(expectedEvent);
                    assertThat(response.getGameId()).isEqualTo(GAME_ID);
                    if (messageFragment != null) {
                        assertThat(response.getMessage()).containsIgnoringCase(messageFragment);
                    }
                })
                .verify();

            // An illegal move must leave no trace: no mutation, no save, no broadcast, no result.
            assertThat(game.getMoves()).hasSize(movesBefore);
            verifyNoInteractions(gameRepository, gameResultRepository, gameSessionManager);
        }
    }

    @Nested
    class RegularMove {

        @Test
        void appliesMoveAndBroadcastsIt() {
            StepVerifier.create(handler.handle(moveEvent(7, 7, 0), game, userA))
                .verifyComplete(); // no direct response; the mover hears back via the broadcast

            assertThat(game.getMoves()).hasSize(1);
            Move placed = game.getMoves().get(0);
            assertThat(placed.x()).isEqualTo(7);
            assertThat(placed.y()).isEqualTo(7);
            assertThat(placed.userId()).isEqualTo(PLAYER_A.getUserId());
            assertThat(game.getLastPlayer()).isEqualTo(PLAYER_A.getUserId());
            assertThat(game.isOver()).isFalse();

            verify(gameRepository).save(game);
            SessionResponse<?> broadcast = captureBroadcasts(1).get(0);
            assertThat(broadcast.getResponseEvent()).isEqualTo(SessionResponseEvent.MOVE_MADE);
            MoveData data = (MoveData) broadcast.getData();
            assertThat(data.move()).isEqualTo(placed);
            assertThat(data.moveCount()).isEqualTo(1);
            verifyNoInteractions(gameResultRepository);
        }

        @Test
        void saveFailurePropagatesWithoutAnyBroadcast() {
            when(gameRepository.save(any(Game.class))).thenReturn(Mono.error(new RuntimeException("mongo down")));

            StepVerifier.create(handler.handle(moveEvent(7, 7, 0), game, userA))
                .verifyErrorMessage("mongo down");

            verifyNoInteractions(gameSessionManager, gameResultRepository);
        }
    }

    @Nested
    class WinningMove {

        @Test
        void completingFiveInARowEndsGameWithWinResult() {
            // A owns (0,0)..(0,3) vertically; B has moved elsewhere. A completes the line at (0,4).
            prefillAlternating(
                new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}},
                new int[][]{{5, 5}, {5, 6}, {5, 7}, {5, 8}});

            StepVerifier.create(handler.handle(moveEvent(0, 4, 8), game, userA))
                .verifyComplete();

            assertThat(game.isOver()).isTrue();

            GameResult result = capturedResult();
            assertThat(result.getResultType()).isEqualTo(GameResult.ResultType.WIN);
            assertThat(result.getWinnerId()).isEqualTo(PLAYER_A.getUserId());
            assertThat(result.getGameId()).isEqualTo(GAME_ID);
            assertThat(result.getMovesCount()).isEqualTo(9);

            List<SessionResponse<?>> broadcasts = captureBroadcasts(2);
            assertThat(broadcasts.get(0).getResponseEvent()).isEqualTo(SessionResponseEvent.MOVE_MADE);
            assertThat(broadcasts.get(1).getResponseEvent()).isEqualTo(SessionResponseEvent.GAME_WON);
        }

        @Test
        void guardSaveHappensBeforeResultInsertAndBroadcasts() {
            prefillAlternating(
                new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}},
                new int[][]{{5, 5}, {5, 6}, {5, 7}, {5, 8}});

            StepVerifier.create(handler.handle(moveEvent(0, 4, 8), game, userA))
                .verifyComplete();

            InOrder order = inOrder(gameRepository, gameSessionManager, gameResultRepository);
            order.verify(gameRepository).save(game);
            order.verify(gameSessionManager).broadcast(eq(GAME_ID), any());
            order.verify(gameResultRepository).save(any(GameResult.class));
            order.verify(gameSessionManager).broadcast(eq(GAME_ID), any());
        }

        @Test
        void winOnTheLastCellIsAWinNotADraw() {
            // 3x3 board, win with 3. Eight cells prefilled so A's (2,2) both completes the
            // (0,0)-(1,1)-(2,2) diagonal and fills the board — the win must take precedence.
            game.setBoardSize(3);
            game.setWinningCount(3);
            prefillAlternating(
                new int[][]{{0, 0}, {1, 1}, {1, 0}, {0, 2}},
                new int[][]{{0, 1}, {1, 2}, {2, 0}, {2, 1}});

            StepVerifier.create(handler.handle(moveEvent(2, 2, 8), game, userA))
                .verifyComplete();

            GameResult result = capturedResult();
            assertThat(result.getResultType()).isEqualTo(GameResult.ResultType.WIN);
            assertThat(result.getWinnerId()).isEqualTo(PLAYER_A.getUserId());
            assertThat(captureBroadcasts(2).get(1).getResponseEvent()).isEqualTo(SessionResponseEvent.GAME_WON);
        }
    }

    @Nested
    class DrawMove {

        @Test
        void fillingTheBoardWithoutAWinnerEndsGameWithDrawResult() {
            // 2x2 board, win with 3 (impossible): the fourth stone fills the board with no winner.
            game.setBoardSize(2);
            game.setWinningCount(3);
            prefillAlternating(
                new int[][]{{0, 0}, {1, 1}},
                new int[][]{{0, 1}});

            StepVerifier.create(handler.handle(moveEvent(1, 0, 3), game, userB))
                .verifyComplete();

            assertThat(game.isOver()).isTrue();

            GameResult result = capturedResult();
            assertThat(result.getResultType()).isEqualTo(GameResult.ResultType.DRAW);
            assertThat(result.getWinnerId()).isNull();
            assertThat(result.getMovesCount()).isEqualTo(4);

            List<SessionResponse<?>> broadcasts = captureBroadcasts(2);
            assertThat(broadcasts.get(0).getResponseEvent()).isEqualTo(SessionResponseEvent.MOVE_MADE);
            assertThat(broadcasts.get(1).getResponseEvent()).isEqualTo(SessionResponseEvent.GAME_DRAW);
        }
    }

    // ========== helpers ==========

    /**
     * Builds a real {@link MoveEvent} the same way production does — by deserializing the wire
     * format (the class has no setters).
     */
    private static MoveEvent moveEvent(int x, int y, long moveSeq) {
        return JSON_SERIALIZER.readValue(
            "{\"type\":\"MOVE\",\"x\":" + x + ",\"y\":" + y + ",\"moveSeq\":" + moveSeq + "}",
            MoveEvent.class);
    }

    /**
     * Prefills the game's move list alternating A, B, A, B... so the turn parity stays realistic.
     * Coordinate arrays are per player, in the order that player made them.
     */
    private void prefillAlternating(int[][] aMoves, int[][] bMoves) {
        int total = aMoves.length + bMoves.length;
        for (int i = 0; i < total; i++) {
            int[] cell = i % 2 == 0 ? aMoves[i / 2] : bMoves[i / 2];
            String userId = i % 2 == 0 ? PLAYER_A.getUserId() : PLAYER_B.getUserId();
            game.getMoves().add(new Move(cell[0], cell[1], userId, i));
        }
    }

    private List<SessionResponse<?>> captureBroadcasts(int expectedCount) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<SessionResponse<?>> captor = ArgumentCaptor.forClass((Class) SessionResponse.class);
        verify(gameSessionManager, times(expectedCount)).broadcast(eq(GAME_ID), captor.capture());
        return captor.getAllValues();
    }

    private GameResult capturedResult() {
        ArgumentCaptor<GameResult> captor = ArgumentCaptor.forClass(GameResult.class);
        verify(gameResultRepository).save(captor.capture());
        return captor.getValue();
    }
}
