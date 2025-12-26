package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.core.base.BaseEntity;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Game extends BaseEntity {

    private Integer boardSize; // number of cells in each director
    private Integer winningCount; // how many pieces in a row to win
    @Builder.Default
    private List<Move> moves = new ArrayList<>();

    @Builder.Default
    private List<UserIdAndName> players = new ArrayList<>(); // players in the game
    @Nullable
    private String lastPlayer; // player who made the last move

    private boolean isOver; // has game ended
}
