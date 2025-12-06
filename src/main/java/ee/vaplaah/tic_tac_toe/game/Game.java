package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.utils.base.BaseEntity;
import jakarta.annotation.Nullable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Game extends BaseEntity {

    private Integer boardSize; // number of cells in each director
    private Integer winningCount; // how many pieces in a row to win
    private String[][] board; // every value is a player's ID

    @Builder.Default
    private List<String> players = new ArrayList<>(); // players in the game
    @Nullable
    private String lastPlayer; // player who made the last move

    private boolean isOver; // has game ended
    private int moves; // number of moves made
}
