package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponse;
import ee.vaplaah.tic_tac_toe.user.User;
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
