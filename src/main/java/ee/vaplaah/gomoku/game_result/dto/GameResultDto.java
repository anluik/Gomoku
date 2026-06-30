package ee.vaplaah.gomoku.game_result.dto;

import ee.vaplaah.gomoku.core.base.BaseDto;
import ee.vaplaah.gomoku.game_result.GameResult;
import ee.vaplaah.gomoku.user.UserIdAndName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultDto extends BaseDto {

    private String gameId;
    private GameResult.ResultType resultType;
    @Builder.Default
    private List<UserIdAndName> players = new ArrayList<>();
    @Nullable
    private String winnerId; // null if draw
    private int movesCount;

    public static GameResultDto from(GameResult gameResult) {
        return GameResultDto.builder()
            .gameId(gameResult.getGameId())
            .resultType(gameResult.getResultType())
            .players(gameResult.getPlayers())
            .winnerId(gameResult.getWinnerId())
            .movesCount(gameResult.getMovesCount())
            .id(gameResult.getId())
            .createdAt(gameResult.getCreatedAt())
            .createdBy(gameResult.getCreatedBy())
            .updatedAt(gameResult.getUpdatedAt())
            .updatedBy(gameResult.getUpdatedBy())
            .build();
    }
}
