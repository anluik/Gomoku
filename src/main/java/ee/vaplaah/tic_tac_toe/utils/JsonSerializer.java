package ee.vaplaah.tic_tac_toe.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import ee.vaplaah.tic_tac_toe.core.exception.JsonSerializationException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;

public enum JsonSerializer {

    JSON_SERIALIZER;

    public final ObjectMapper objectMapper;

    JsonSerializer() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(consistentMillisecondsTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setVisibility(FIELD, ANY);
    }

    public <T> T readValue(String json, Class<T> classType) {
        try {
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, classType);
        } catch (Exception e) {
            if (e instanceof JsonMappingException jsonException) {
                throw new JsonSerializationException("Unable to deserialize due to: " + jsonException.getPathReference());
            }
            throw new JsonSerializationException("Unable to deserialize JSON to ".concat(classType.getName()));
        }
    }

    /**
     * Serializes an object to a JSON string, removing any Unicode NUL characters.
     */
    public <T> String writeAsJson(T object) {
        try {
            String jsonString = objectMapper.writeValueAsString(object);
            return jsonString.replace("\\u0000", "");
        } catch (Exception e) {
            throw new JsonSerializationException("Unable to serialize object to JSON".concat(object.getClass().getName()));
        }
    }

    public <T> T convertValue(Object object, Class<T> targetClass) {
        return objectMapper.convertValue(object, targetClass);
    }

    public <T> T readValue(String json, TypeReference<T> typeReference) {
        try {
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            if (e instanceof JsonMappingException jsonException) {
                throw new JsonSerializationException("Unable to deserialize due to: " + jsonException.getPathReference());
            }
            throw new JsonSerializationException("Unable to deserialize JSON to ".concat(typeReference.getType().getTypeName()));
        }
    }

    public <T> T convertValue(Object object, TypeReference<T> typeReference) {
        return objectMapper.convertValue(object, typeReference);
    }

    private static JavaTimeModule consistentMillisecondsTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        ZonedDateTimeSerializer dateTimeSerializer = new ZonedDateTimeSerializer(
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter());
        javaTimeModule.addSerializer(ZonedDateTime.class, dateTimeSerializer);

        return javaTimeModule;
    }
}
