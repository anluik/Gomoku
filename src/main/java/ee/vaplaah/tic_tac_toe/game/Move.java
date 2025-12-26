package ee.vaplaah.tic_tac_toe.game;

import lombok.Builder;

@Builder
public record Move(int x, int y, String userId, long timestamp) {}
