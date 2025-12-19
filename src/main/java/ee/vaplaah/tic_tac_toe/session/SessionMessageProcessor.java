package ee.vaplaah.tic_tac_toe.session;

import ee.vaplaah.tic_tac_toe.core.exception.JsonSerializationException;
import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.core.exception.types.RequestViolation;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.InvalidRequestSessionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static ee.vaplaah.tic_tac_toe.utils.JsonSerializer.JSON_SERIALIZER;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionMessageProcessor {

    private final Validator validator;

    /**
     * Attempts to read and validate a message.
     * If successful, returns Mono.just(T).
     * If failed, sends an error response and returns Mono.empty().
     */
    public <T> Mono<T> process(WebSocketSession session, String textPayload, Class<T> messageType) {
        T message;

        try {
            message = JSON_SERIALIZER.readValue(textPayload, messageType);
        } catch (JsonSerializationException e) {
            return sendGenericError(session, "Invalid message format or type mismatch");
        } catch (Exception e) {
            log.error("Unexpected error during message parsing", e);
            return sendGenericError(session, "Internal parsing error");
        }

        if (message == null) {
            return sendGenericError(session, "Received empty or null message payload");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            return sendValidationErrors(session, violations);
        }

        return Mono.just(message);
    }

    private <T> Mono<T> sendValidationErrors(WebSocketSession session, Set<ConstraintViolation<T>> violations) {
        List<RequestViolation> mappedViolations = violations.stream()
            .map(violation -> RequestViolation.builder()
                .field(violation.getPropertyPath().toString())
                .error(violation.getMessage())
                .build())
            .toList();

        InvalidRequestSessionResponse response = InvalidRequestSessionResponse.builder()
            .message("Validation failed")
            .violations(mappedViolations)
            .build();

        String errorResponseJson = JSON_SERIALIZER.writeAsJson(response);
        return session.send(Mono.just(session.textMessage(errorResponseJson)))
            .then(Mono.empty());
    }

    private <T> Mono<T> sendGenericError(WebSocketSession session, String message) {
        BaseSessionResponse response = BaseSessionResponse.builder()
            .message(message)
            .status(ResponseStatus.ERROR)
            .build();

        String errorResponseJson = JSON_SERIALIZER.writeAsJson(response);
        return session.send(Mono.just(session.textMessage(errorResponseJson)))
            .then(Mono.empty());
    }
}