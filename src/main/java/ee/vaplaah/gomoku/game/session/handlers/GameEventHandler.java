package ee.vaplaah.gomoku.game.session.handlers;

import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.user.User;
import reactor.core.publisher.Mono;

public interface GameEventHandler {

    Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user);
    boolean supports(GameEventType eventType);

    /**
     * Indicates whether the event requires the game to be active (not over).
     */
    default boolean requiresActiveGame() { return true; }
    /**
     * Indicates whether the event requires the user to be a participant in the game.
     */
    default boolean requiresParticipant() { return true; }
}
