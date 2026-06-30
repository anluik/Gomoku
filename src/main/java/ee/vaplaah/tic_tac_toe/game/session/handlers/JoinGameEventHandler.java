package ee.vaplaah.tic_tac_toe.game.session.handlers;

import static ee.vaplaah.tic_tac_toe.utils.GameUtils.isUserPartOfGame;

import ee.vaplaah.tic_tac_toe.core.exception.SessionMessageProcessingException;
import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.game.GameRepository;
import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.game.session.GameSessionManager;
import ee.vaplaah.tic_tac_toe.session.message.GameEvent;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.game.GameResponses;
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
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        String gameId = event.getGameId();
        return validateUserNotInGame(game, user)
            .then(validateMaxPlayers(game))
            .then(Mono.defer(() -> addUserToTheGame(game, user)
                .flatMap(savedGame -> broadcastUserJoinedEvent(user, savedGame, gameId))));
    }

    // TODO: is this needed? Just ignore the event and return success.
    private Mono<SessionResponse<?>> validateUserNotInGame(Game game, User user) {
        if (isUserPartOfGame(game.getPlayers(), user.getId())) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.userAlreadyJoined(game.getId(), game.getPlayers())));
        }
        return Mono.empty();
    }

    private Mono<SessionResponse<?>> validateMaxPlayers(Game game) {
        if (game.getPlayers().size() >= MAX_PLAYERS) {
            return Mono.error(new SessionMessageProcessingException(
                GameResponses.gameFull(game.getId(), game.getPlayers())));
        }
        return Mono.empty();
    }

    private Mono<Game> addUserToTheGame(Game game, User user) {
        game.getPlayers().add(UserIdAndName.fromUser(user));
        return gameRepository.save(game);
    }

    private Mono<SessionResponse<?>> broadcastUserJoinedEvent(User user, Game savedGame, String gameId) {
        SessionResponse<?> broadcastMessage = GameResponses.userJoined(
            gameId, UserIdAndName.fromUser(user), savedGame.getPlayers());

        // Broadcast to all subscribers of this game
        gameSessionManager.broadcast(gameId, broadcastMessage);

        // Empty direct response to websocket message. Update is communicated via broadcast.
        return Mono.empty();
    }
}
