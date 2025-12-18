package ee.vaplaah.tic_tac_toe.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.BaseAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        log.error("[JwtAuthenticationEntryPoint] Caught authentication exception with message: {}", ex.getMessage());
        String reason = "Authentication required";

        if (ex instanceof BaseAuthenticationException baseEx) {
            reason = baseEx.getMessage();
            exchange.getResponse().setStatusCode(baseEx.getHttpStatus());
        } else {
            exchange.getResponse().setStatusCode(UNAUTHORIZED);
        }

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("message", reason);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
