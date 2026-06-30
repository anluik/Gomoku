package ee.vaplaah.tic_tac_toe.game_result;

import ee.vaplaah.tic_tac_toe.core.base.BaseEntity;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
@Document
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GameResult extends BaseEntity {

    @Indexed
    private String gameId;
    private ResultType resultType;
    @Builder.Default
    private List<UserIdAndName> players = new ArrayList<>();
    @Nullable
    private String winnerId; // null if draw or abort
    private int movesCount;

    public enum ResultType {
        WIN,
        DRAW,
        RESIGN,
        /**
         * Game is over without result.
         * Examples:
         * - Game started, but player didn't make their first move in time.
         * - Player manually aborted the game before the first move.
         * - Both players disconnected.
         */
        ABORT
    }
}
