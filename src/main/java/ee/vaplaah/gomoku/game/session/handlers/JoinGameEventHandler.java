package ee.vaplaah.gomoku.game.session.handlers;

import static ee.vaplaah.gomoku.utils.GameUtils.isUserPartOfGame;

import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.game.Game;
import ee.vaplaah.gomoku.game.GameRepository;
import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.game.session.GameSessionManager;
import ee.vaplaah.gomoku.session.message.GameEvent;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.game.GameResponses;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserIdAndName;
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

    @Override
    public boolean retryOnConflict() {
        return true;
    }

    @Transactional
    @Override
    public Mono<SessionResponse<?>> handle(GameEvent event, Game game, User user) {
        String gameId = game.getId();
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
        // TODO: think about which color the user joined as
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
