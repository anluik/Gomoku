package ee.vaplaah.gomoku.game;

import lombok.Builder;

@Builder
public record Move(int x, int y, String userId, long timestamp) {}
