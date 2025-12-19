package ee.vaplaah.tic_tac_toe.core.exception.types;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ErrorCode;
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