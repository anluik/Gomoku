package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.user.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserJoinedEventHandler implements GameEventHandler {

    @Override
    public Mono<BaseSessionResponse<?>> handle(GameEvent event, User user) {
        // 1. get game from DB
        // 2. check that there aren't more than 2 players
        // 3. update players list
        // 4. broadcast user joined event
        return Mono.just(
            BaseSessionResponse.builder()
                .status(ResponseStatus.SUCCESS)
//                .responseEvent(event.getType().getSuccessResponseEvent())
                .message("User joined event handled")
                .build()
        );
    }

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.USER_JOINED;
    }
}
