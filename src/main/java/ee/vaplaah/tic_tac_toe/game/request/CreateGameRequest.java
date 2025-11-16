package ee.vaplaah.tic_tac_toe.game.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateGameRequest {

    @NotEmpty
    private String creatorId;
    @NotNull
    @Size(min = 3, max = 9)
    private Integer boardSize;
    @NotNull
    @Size(min = 3, max = 9)
    private Integer winningCount;

}
