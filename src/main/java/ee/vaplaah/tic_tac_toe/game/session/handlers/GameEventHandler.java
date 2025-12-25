package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.user.User;
import reactor.core.publisher.Mono;

public interface GameEventHandler {

    Mono<BaseSessionResponse<?>> handle(GameEvent event, User user);
    boolean supports(GameEventType eventType);
}
