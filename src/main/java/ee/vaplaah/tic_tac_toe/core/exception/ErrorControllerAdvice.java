package ee.vaplaah.tic_tac_toe.core.exception;

import ee.vaplaah.tic_tac_toe.core.exception.enums.ErrorCode;
import ee.vaplaah.tic_tac_toe.core.exception.types.ApiErrorResponse;
import ee.vaplaah.tic_tac_toe.core.exception.types.ApiErrorResponse.ApiErrorResponseBuilder;
import ee.vaplaah.tic_tac_toe.core.exception.types.RequestViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.List;

import static ee.vaplaah.tic_tac_toe.utils.JsonSerializer.JSON_SERIALIZER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
public class ErrorControllerAdvice {

    @ExceptionHandler(CommonException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleCommonException(CommonException e) {
        ApiErrorResponseBuilder response = ApiErrorResponse.builder()
            .code(e.getErrorCode())
            .message(e.getMessage());
        if (e.getDetails() != null) {
            String detailsJson = JSON_SERIALIZER.writeAsJson(e.getDetails());
            response.details(detailsJson);
        }
        return Mono.just(new ResponseEntity<>(response.build(), BAD_REQUEST));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleWebExchangeBindException(WebExchangeBindException ex) {
        ApiErrorResponseBuilder response = ApiErrorResponse.builder()
            .code(ErrorCode.INVALID_REQUEST)
            .message("Validation failed");
        List<RequestViolation> violations = ex.getBindingResult().getAllErrors().stream()
            .map(error -> RequestViolation.builder()
                .field(((FieldError) error).getField())
                .error(error.getDefaultMessage())
                .build())
            .toList();
        if (!violations.isEmpty()) {
            String violationsJson = JSON_SERIALIZER.writeAsJson(violations);
            response.details(violationsJson);
        }
        return Mono.just(new ResponseEntity<>(response.build(), BAD_REQUEST));
    }
}
