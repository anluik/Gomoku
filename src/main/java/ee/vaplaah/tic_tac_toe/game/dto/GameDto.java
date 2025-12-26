package ee.vaplaah.tic_tac_toe.game.dto;

import ee.vaplaah.tic_tac_toe.game.Game;
import ee.vaplaah.tic_tac_toe.core.base.BaseDto;
import ee.vaplaah.tic_tac_toe.user.UserIdAndName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto extends BaseDto {

    private Integer boardSize;
    private Integer winningCount;
    private String[][] board;

    @Builder.Default
    private List<UserIdAndName> players = List.of();
    private String lastPlayer;

    private boolean isOver;

    public static GameDto from(Game game) {
        return GameDto.builder()
            .id(game.getId())
            .boardSize(game.getBoardSize())
            .winningCount(game.getWinningCount())
            .board(game.getBoard())
            .players(game.getPlayers())
            .lastPlayer(game.getLastPlayer())
            .isOver(game.isOver())
            .createdAt(game.getCreatedAt())
            .createdBy(game.getCreatedBy())
            .updatedAt(game.getUpdatedAt())
            .updatedBy(game.getUpdatedBy())
            .build();
    }
}
