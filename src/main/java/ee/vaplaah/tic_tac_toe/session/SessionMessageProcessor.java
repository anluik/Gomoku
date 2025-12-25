package ee.vaplaah.tic_tac_toe.session;

import ee.vaplaah.tic_tac_toe.core.exception.JsonSerializationException;
import ee.vaplaah.tic_tac_toe.core.exception.SessionMessageProcessingException;
import ee.vaplaah.tic_tac_toe.core.exception.enums.ResponseStatus;
import ee.vaplaah.tic_tac_toe.core.exception.types.RequestViolation;
import ee.vaplaah.tic_tac_toe.session.response.BaseSessionResponse;
import ee.vaplaah.tic_tac_toe.session.response.InvalidPayloadSessionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
                BaseSessionResponse<?> response = createGenericErrorResponse("Empty payload");
                return Mono.error(new SessionMessageProcessingException(response));
            }

            Set<ConstraintViolation<T>> violations = validator.validate(message);
            if (!violations.isEmpty()) {
                BaseSessionResponse<?> response = createValidationErrorResponse(violations);
                return Mono.error(new SessionMessageProcessingException(response));
            }

            return Mono.just(message);
        } catch (JsonSerializationException e) {
            BaseSessionResponse<?> response = createGenericErrorResponse("Invalid message format or type mismatch");
            return Mono.error(new SessionMessageProcessingException(response));
        } catch (Exception e) {
            log.error("Unexpected error during message parsing", e);
            BaseSessionResponse<?> response = createGenericErrorResponse("Internal error");
            return Mono.error(new SessionMessageProcessingException(response));
        }
    }

    private <T> InvalidPayloadSessionResponse createValidationErrorResponse(Set<ConstraintViolation<T>> violations) {
        List<RequestViolation> mappedViolations = violations.stream()
            .map(violation -> RequestViolation.builder()
                .field(violation.getPropertyPath().toString())
                .error(violation.getMessage())
                .build())
            .toList();

        return InvalidPayloadSessionResponse.builder()
            .message("Validation failed")
            .data(mappedViolations)
            .build();
    }

    private BaseSessionResponse<?> createGenericErrorResponse(String message) {
        return BaseSessionResponse.builder()
            .message(message)
            .status(ResponseStatus.ERROR)
            .build();
    }
}