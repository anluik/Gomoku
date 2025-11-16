package ee.vaplaah.tic_tac_toe.stomp;

import ee.vaplaah.tic_tac_toe.enums.GameEventType;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Getter
public class GameEvent {

    @DBRef
    private String gameId;

    GameEventType type;
}
