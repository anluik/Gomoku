package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.session.message.ChatEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.session.response.game.payload.ChatData;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static ee.vaplaah.gomoku.utils.JsonSerializer.JSON_SERIALIZER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ChatEventHandler}: pure fan-out — no game-state mutation, no persistence,
 * open to spectators and finished games.
 */
@ExtendWith(MockitoExtension.class)
class ChatEventHandlerTest {

    private static final String GAME_ID = "game-1";

    @Mock private GameSessionManager gameSessionManager;

    private ChatEventHandler handler;
    private Game game;
    private User spectator;

    @BeforeEach
    void setUp() {
        handler = new ChatEventHandler(gameSessionManager);
        spectator = User.builder().id("user-s").username("sam").build();
        game = Game.builder()
            .id(GAME_ID)
            .boardSize(15)
            .winningCount(5)
            .players(new ArrayList<>(List.of(new UserIdAndName("user-a", "alice"))))
            .build();
    }

    @Test
    void declaresItsDispatchContract() {
        assertThat(handler.supports(GameEventType.CHAT)).isTrue();
        assertThat(handler.supports(GameEventType.MOVE)).isFalse();
        // Chat is open to spectators and keeps working after the game ends.
        assertThat(handler.requiresActiveGame()).isFalse();
        assertThat(handler.requiresParticipant()).isFalse();
        // No state mutation → no optimistic-lock retry.
        assertThat(handler.retryOnConflict()).isFalse();
    }

    @Test
    void broadcastsChatMessageWithoutTouchingGameState() {
        ChatEvent chat = JSON_SERIALIZER.readValue(
            "{\"type\":\"CHAT\",\"text\":\"good game!\"}", ChatEvent.class);
        boolean overBefore = game.isOver();
        int playersBefore = game.getPlayers().size();

        StepVerifier.create(handler.handle(chat, game, spectator))
            .verifyComplete(); // sender hears their own message back via the broadcast

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<SessionResponse<?>> captor = ArgumentCaptor.forClass((Class) SessionResponse.class);
        verify(gameSessionManager).broadcast(eq(GAME_ID), captor.capture());
        SessionResponse<?> broadcast = captor.getValue();
        assertThat(broadcast.getResponseEvent()).isEqualTo(SessionResponseEvent.CHAT_MESSAGE);
        ChatData data = (ChatData) broadcast.getData();
        assertThat(data.sender().getUserId()).isEqualTo("user-s");
        assertThat(data.sender().getUsername()).isEqualTo("sam");
        assertThat(data.text()).isEqualTo("good game!");

        assertThat(game.isOver()).isEqualTo(overBefore);
        assertThat(game.getPlayers()).hasSize(playersBefore);
    }
}
