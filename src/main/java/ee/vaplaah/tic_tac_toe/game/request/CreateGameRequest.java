package ee.vaplaah.tic_tac_toe.game.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateGameRequest {

    @NotNull(message = "Board size is required")
    @Min(value = 3, message = "Board size must be at least 3")
    @Max(value = 9, message = "Board size must be at most 9")
    private Integer boardSize;
    @NotNull(message = "Winning count is required")
    @Min(value = 3, message = "Winning count must be at least 3")
    @Max(value = 9, message = "Winning count must be at most 9")
    private Integer winningCount;

    @AssertTrue(message = "Winning count must be less than or equal to board size")
    public boolean isWinningCountValid() {
        if (winningCount == null || boardSize == null) {
            return true; // Other validations will catch null values
        }
        return winningCount <= boardSize;
    }
}
