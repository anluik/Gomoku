package ee.vaplaah.tic_tac_toe.game.dto;

import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.utils.base.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@SuperBuilder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class GameDto extends BaseDto {

    private String id;
    private Integer boardSize;
    private Integer winningCount;
    private String[][] board;

    @Builder.Default
    private List<String> players = List.of();
    private String lastPlayer;

    private boolean isOver;
    private int moves;

    public static GameDto from(Game game) {
        return GameDto.builder()
            .id(game.getId())
            .boardSize(game.getBoardSize())
            .winningCount(game.getWinningCount())
            .board(game.getBoard())
            .players(game.getPlayers())
            .lastPlayer(game.getLastPlayer())
            .isOver(game.isOver())
            .moves(game.getMoves())
            .build();
    }
}
