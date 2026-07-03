package ee.vaplaah.gomoku.session.response.game;

import ee.vaplaah.gomoku.game.session.GameEventType;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import ee.vaplaah.gomoku.session.response.SessionResponseEvent;
import ee.vaplaah.gomoku.game.Move;
import ee.vaplaah.gomoku.session.response.game.payload.ChatData;
import ee.vaplaah.gomoku.session.response.game.payload.GameOutcomeData;
import ee.vaplaah.gomoku.session.response.game.payload.GameStartedData;
import ee.vaplaah.gomoku.session.response.game.payload.MoveData;
import ee.vaplaah.gomoku.session.response.game.payload.PresenceData;
import ee.vaplaah.gomoku.session.response.game.payload.UserJoinedData;
import ee.vaplaah.gomoku.session.response.game.payload.UserResignedData;
import ee.vaplaah.gomoku.user.UserIdAndName;
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

    public SessionResponse<GameStartedData> gameStarted(String gameId, List<UserIdAndName> players) {
        return SessionResponse.<GameStartedData>success(SessionResponseEvent.GAME_STARTED, "Game started")
            .gameId(gameId)
            .data(new GameStartedData(players, players.get(0).getUserId()))
            .build();
    }

    public SessionResponse<PresenceData> playerDisconnected(String gameId, UserIdAndName player, int graceSeconds) {
        return SessionResponse.<PresenceData>success(SessionResponseEvent.PLAYER_DISCONNECTED,
                "Player " + player.getUsername() + " disconnected")
            .gameId(gameId)
            .data(new PresenceData(player, false, graceSeconds))
            .build();
    }

    public SessionResponse<PresenceData> playerReconnected(String gameId, UserIdAndName player) {
        return SessionResponse.<PresenceData>success(SessionResponseEvent.PLAYER_RECONNECTED,
                "Player " + player.getUsername() + " reconnected")
            .gameId(gameId)
            .data(new PresenceData(player, true, null))
            .build();
    }

    public SessionResponse<GameOutcomeData> gameAborted(String gameId) {
        return SessionResponse.<GameOutcomeData>success(SessionResponseEvent.GAME_ABORTED, "Game aborted")
            .gameId(gameId)
            .data(new GameOutcomeData(null, true))
            .build();
    }

    public SessionResponse<UserResignedData> userResigned(String gameId, UserIdAndName player, String winnerId, boolean over) {
        return SessionResponse.<UserResignedData>success(SessionResponseEvent.USER_RESIGNED,
                "User " + player.getUsername() + " resigned")
            .gameId(gameId)
            .data(new UserResignedData(player, winnerId, over))
            .build();
    }

    public SessionResponse<MoveData> moveMade(String gameId, Move move, int moveCount) {
        return SessionResponse.<MoveData>success(SessionResponseEvent.MOVE_MADE,
                "Move placed at (" + move.x() + ", " + move.y() + ")")
            .gameId(gameId)
            .data(new MoveData(move, moveCount))
            .build();
    }

    public SessionResponse<GameOutcomeData> gameWon(String gameId, String winnerId) {
        return SessionResponse.<GameOutcomeData>success(SessionResponseEvent.GAME_WON, "Game won")
            .gameId(gameId)
            .data(new GameOutcomeData(winnerId, true))
            .build();
    }

    public SessionResponse<GameOutcomeData> gameDrawn(String gameId) {
        return SessionResponse.<GameOutcomeData>success(SessionResponseEvent.GAME_DRAW, "Game drawn")
            .gameId(gameId)
            .data(new GameOutcomeData(null, true))
            .build();
    }

    public SessionResponse<ChatData> chatMessage(String gameId, UserIdAndName sender, String text) {
        return SessionResponse.<ChatData>success(SessionResponseEvent.CHAT_MESSAGE,
                "Chat message from " + sender.getUsername())
            .gameId(gameId)
            .data(new ChatData(sender, text))
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

    public SessionResponse<Void> gameNotReady(String gameId) {
        return SessionResponse.<Void>error(SessionResponseEvent.GAME_NOT_READY, "Waiting for both players to join")
            .gameId(gameId)
            .build();
    }

    public SessionResponse<Void> notYourTurn(String gameId) {
        return SessionResponse.<Void>error(SessionResponseEvent.NOT_YOUR_TURN, "It is not your turn")
            .gameId(gameId)
            .build();
    }

    public SessionResponse<Void> invalidMove(String gameId, String reason) {
        return SessionResponse.<Void>error(SessionResponseEvent.INVALID_MOVE, reason)
            .gameId(gameId)
            .build();
    }

    public SessionResponse<Void> abortNotAllowed(String gameId, String reason) {
        return SessionResponse.<Void>error(SessionResponseEvent.ABORT_NOT_ALLOWED, reason)
            .gameId(gameId)
            .build();
    }

    public SessionResponse<Void> resignNotAllowed(String gameId, String reason) {
        return SessionResponse.<Void>error(SessionResponseEvent.RESIGN_NOT_ALLOWED, reason)
            .gameId(gameId)
            .build();
    }
}
