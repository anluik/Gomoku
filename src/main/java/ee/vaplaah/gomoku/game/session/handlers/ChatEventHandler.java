package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.session.message.ChatEvent;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Chat is a pure fan-out: it mutates no game state, so it needs neither optimistic-lock retry nor a
 * DB write. Anyone connected to the game (player or spectator) may chat, whether or not the game is
 * still active.
 */
@Component
@RequiredArgsConstructor
public class ChatEventHandler implements GameEventHandler {

    private final GameSessionManager gameSessionManager;

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.CHAT;
    }

    @Override
    public boolean requiresActiveGame() {
        return false;
    }

    @Override
    public boolean requiresParticipant() {
        return false;
    }

    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        ChatEvent chat = (ChatEvent) event;
        gameSessionManager.broadcast(game.getId(),
            GameResponses.chatMessage(game.getId(), UserIdAndName.fromUser(user), chat.getText()));
        return Mono.empty();
    }
}
