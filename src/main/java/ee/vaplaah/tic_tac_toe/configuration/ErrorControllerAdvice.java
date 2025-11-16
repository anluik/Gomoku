package ee.vaplaah.tic_tac_toe.configuration;

import ee.vaplaah.tic_tac_toe.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@Slf4j
@ControllerAdvice
public class ErrorControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<Map<String, Object>> handleCommonException(CommonException e) {
        return new ResponseEntity<>(
            Map.of("message", e.getMessage(), "fieldErrors", e.getFieldErrors()),
            e.getHttpStatus());
    }
}
