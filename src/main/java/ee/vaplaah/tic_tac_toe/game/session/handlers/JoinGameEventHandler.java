package ee.vaplaah.tic_tac_toe.game.session.handlers;

import ee.vaplaah.tic_tac_toe.core.exception.SessionMessageProcessingException;
import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.game.GameRepository;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.game.session.GameSessionManager;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponseEvent;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JoinGameEventHandler implements GameEventHandler {

    public static final int MAX_PLAYERS = 2;

    private final GameSessionManager gameSessionManager;
    private final GameRepository gameRepository;

    @Override
    public boolean supports(GameEventType eventType) {
        return eventType == GameEventType.JOIN_GAME;
    }

    @Override
    public boolean requiresParticipant() {
        return false;
    }

    @Transactional
    @Override
    public Mono<BaseSessionResponse<?>> handle(GameEvent event, Game game, User user) {
        String gameId = event.getGameId();
        return validateMaxPlayers(game)
            .then(Mono.defer(() -> addUserToTheGame(game, user)
                .flatMap(savedGame -> broadcastUserJoinedEvent(user, savedGame, gameId))));
    }

    private Mono<BaseSessionResponse<?>> validateMaxPlayers(Game game) {
        if (game.getPlayers().size() >= MAX_PLAYERS) {
            BaseSessionResponse<?> response = BaseSessionResponse.builder()
                .status(ResponseStatus.ERROR)
                .responseEvent(SessionResponseEvent.GAME_FULL)
                .message("Unable to join game - maximum players reached")
                .build();
            return Mono.error(new SessionMessageProcessingException(response));
        }
        return Mono.empty();
    }

    private Mono<Game> addUserToTheGame(Game game, User user) {
        game.getPlayers().add(UserIdAndName.fromUser(user));
        return gameRepository.save(game);
    }

    private Mono<BaseSessionResponse<?>> broadcastUserJoinedEvent(User user, Game savedGame, String gameId) {
        var broadcastMessage = BaseSessionResponse.builder()
            .status(ResponseStatus.SUCCESS)
            .responseEvent(SessionResponseEvent.USER_JOINED)
            .message("User " + user.getUsername() + " joined the game")
            .data(savedGame)
            .build();

        // Broadcast to all subscribers of this game
        gameSessionManager.broadcast(gameId, broadcastMessage);

        // Empty direct response to websocket message. Update is communicated via broadcast.
        return Mono.empty();
    }
}
