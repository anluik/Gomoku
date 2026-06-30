package ee.vaplaah.gomoku.core.exception.types;

import ee.vaplaah.gomoku.core.exception.enums.ErrorCode;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.lang.Nullable;

@Builder
public record ApiErrorResponse(
    @NonNull
    ErrorCode code,
    @NonNull
    String message,
    @Nullable
    String details
) {}