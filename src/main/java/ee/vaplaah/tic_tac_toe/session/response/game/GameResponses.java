package ee.vaplaah.tic_tac_toe.session.response.game;

import ee.vaplaah.tic_tac_toe.game.session.GameEventType;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.SessionResponseEvent;
import ee.vaplaah.tic_tac_toe.session.response.game.payload.GameOutcomeData;
import ee.vaplaah.tic_tac_toe.session.response.game.payload.UserJoinedData;
import ee.vaplaah.tic_tac_toe.session.response.game.payload.UserLeftData;
import ee.vaplaah.tic_tac_toe.session.response.game.payload.UserResignedData;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Factories for every game-scoped {@link SessionResponse}. Lives next to the game payload
 * DTOs so adding a game event touches only this package, never the shared envelope.
 */
@UtilityClass
public class GameResponses {

    // ========== Success responses ==========

    public SessionResponse<UserJoinedData> userJoined(String gameId, UserIdAndName player, List<UserIdAndName> players) {
        return SessionResponse.<UserJoinedData>success(SessionResponseEvent.USER_JOINED,
                "User " + player.getUsername() + " joined the game")
            .gameId(gameId)
            .data(new UserJoinedData(player, players))
            .build();
    }

    public SessionResponse<UserLeftData> userLeft(String gameId, UserIdAndName player, List<UserIdAndName> players, boolean over) {
        return SessionResponse.<UserLeftData>success(SessionResponseEvent.USER_LEFT,
                "User " + player.getUsername() + " left the game")
            .gameId(gameId)
            .data(new UserLeftData(player, players, over))
            .build();
    }

    public SessionResponse<UserResignedData> userResigned(String gameId, UserIdAndName player, String winnerId, boolean over) {
        return SessionResponse.<UserResignedData>success(SessionResponseEvent.USER_RESIGNED,
                "User " + player.getUsername() + " resigned")
            .gameId(gameId)
            .data(new UserResignedData(player, winnerId, over))
            .build();
    }

    // ========== Game errors ==========

    public SessionResponse<List<UserIdAndName>> gameFull(String gameId, List<UserIdAndName> players) {
        return SessionResponse.<List<UserIdAndName>>error(SessionResponseEvent.GAME_FULL,
                "Unable to join game - maximum players reached")
            .gameId(gameId)
            .data(players)
            .build();
    }

    public SessionResponse<List<UserIdAndName>> userAlreadyJoined(String gameId, List<UserIdAndName> players) {
        return SessionResponse.<List<UserIdAndName>>error(SessionResponseEvent.USER_ALREADY_JOINED,
                "Unable to join game - user already in game")
            .gameId(gameId)
            .data(players)
            .build();
    }

    public SessionResponse<GameOutcomeData> gameAlreadyOver(String gameId, GameOutcomeData outcome) {
        return SessionResponse.<GameOutcomeData>error(SessionResponseEvent.GAME_ALREADY_OVER, "Game has ended")
            .gameId(gameId)
            .data(outcome)
            .build();
    }

    public SessionResponse<Void> userNotPartOfTheGame(String gameId) {
        return SessionResponse.<Void>error(SessionResponseEvent.USER_NOT_PART_OF_THE_GAME, "User not part of the game")
            .gameId(gameId)
            .build();
    }

    public SessionResponse<Void> gameNotFound(String gameId) {
        return SessionResponse.<Void>error(SessionResponseEvent.GAME_NOT_FOUND, "Game not found")
            .gameId(gameId)
            .build();
    }

    public SessionResponse<Void> unsupportedEvent(String gameId, GameEventType type) {
        return SessionResponse.<Void>error(SessionResponseEvent.UNSUPPORTED_EVENT, "Unsupported event type: " + type)
            .gameId(gameId)
            .build();
    }
}
