package ee.vaplaah.gomoku.session;

import ee.vaplaah.gomoku.core.exception.JsonSerializationException;
import ee.vaplaah.gomoku.core.exception.SessionMessageProcessingException;
import ee.vaplaah.gomoku.core.exception.types.RequestViolation;
import ee.vaplaah.gomoku.session.response.CommonResponses;
import ee.vaplaah.gomoku.session.response.SessionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static ee.vaplaah.gomoku.utils.JsonSerializer.JSON_SERIALIZER;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionMessageProcessor {

    private final Validator validator;

    /**
     * Processes and validates the incoming text payload into the specified message type.
     * @param textPayload the incoming message as text.
     * @param messageType the class type to deserialize the message into.
     * @return Mono of the validated message type or error Mono with SessionMessageProcessingException.
     * @param <T> The type of the message to process.
     */
    public <T> Mono<T> process(String textPayload, Class<T> messageType) {
        try {
            T message = JSON_SERIALIZER.readValue(textPayload, messageType);

            if (message == null) {
                SessionResponse<?> response = createGenericErrorResponse("Empty payload");
                return Mono.error(new SessionMessageProcessingException(response));
            }

            Set<ConstraintViolation<T>> violations = validator.validate(message);
            if (!violations.isEmpty()) {
                SessionResponse<?> response = createValidationErrorResponse(violations);
                return Mono.error(new SessionMessageProcessingException(response));
            }

            return Mono.just(message);
        } catch (JsonSerializationException e) {
            SessionResponse<?> response = createGenericErrorResponse("Invalid message format or type mismatch");
            return Mono.error(new SessionMessageProcessingException(response));
        } catch (Exception e) {
            log.error("Unexpected error during message parsing", e);
            return Mono.error(new SessionMessageProcessingException(CommonResponses.unexpected()));
        }
    }

    private <T> SessionResponse<?> createValidationErrorResponse(Set<ConstraintViolation<T>> violations) {
        List<RequestViolation> mappedViolations = violations.stream()
            .map(violation -> RequestViolation.builder()
                .field(violation.getPropertyPath().toString())
                .error(violation.getMessage())
                .build())
            .toList();

        return CommonResponses.validationFailed(mappedViolations);
    }

    private SessionResponse<?> createGenericErrorResponse(String message) {
        return CommonResponses.malformedPayload(message);
    }
}