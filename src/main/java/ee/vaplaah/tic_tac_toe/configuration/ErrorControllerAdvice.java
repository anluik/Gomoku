package ee.vaplaah.tic_tac_toe.configuration;

import ee.vaplaah.tic_tac_toe.exception.CommonException;
import ee.vaplaah.tic_tac_toe.exception.types.RequestViolation;
import ee.vaplaah.tic_tac_toe.session.response.BaseResponse;
import ee.vaplaah.tic_tac_toe.session.response.InvalidRequestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestControllerAdvice
public class ErrorControllerAdvice {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<BaseResponse> handleCommonException(CommonException e) {
        BaseResponse response;
        if (!e.getViolations().isEmpty()) {
            response = InvalidRequestResponse.builder()
                .message("Validation failed")
                .violations(e.getViolations())
                .build();
            return new ResponseEntity<>(response, e.getHttpStatus());
        }

        response = BaseResponse.builder()
            .message(e.getMessage())
            .build();
        return new ResponseEntity<>(response, e.getHttpStatus());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<InvalidRequestResponse>> handleWebExchangeBindException(WebExchangeBindException ex) {
        List<RequestViolation> violations = ex.getBindingResult().getAllErrors().stream()
            .map(error -> RequestViolation.builder()
                .field(((FieldError) error).getField())
                .error(error.getDefaultMessage())
                .build())
            .toList();

        InvalidRequestResponse response = InvalidRequestResponse.builder()
            .message("Validation failed")
            .violations(violations)
            .build();

        return Mono.just(new ResponseEntity<>(response, BAD_REQUEST));
    }
}
