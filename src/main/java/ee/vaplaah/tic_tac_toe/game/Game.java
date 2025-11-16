package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.utils.base.BaseEntity;
import jakarta.annotation.Nullable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Builder
@Document
@NoArgsConstructor
@AllArgsConstructor
public class Game extends BaseEntity {

    @Id
    private String id;
    private Integer boardSize; // number of cells in each director
    private Integer winningCount; // how many pieces in a row to win
    private String[][] board; // every value is a player's ID

    @DBRef
    @Builder.Default
    private List<String> players = List.of(); // players in the game
    @Nullable
    private String lastPlayer; // player who made the last move

    private boolean isOver; // has game ended
    private int moves; // number of moves made
}
