package ee.vaplaah.gomoku.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Handles {@link AccessDeniedException} for authenticated
 * users who attempt to reach a resource that their granted roles do not permit (HTTP 403).
 *
 * <p>Spring Security distinguishes between two failure modes:
 * a missing/invalid identity (401 — handled by {@link JwtAuthenticationEntryPoint}) and a
 * valid identity that lacks the required permission (403 — handled here). Without this bean,
 * the framework would fall back to its default HTML error page.
 * </p>
 *
 * <p>Invoked by the Spring Security exception translation filter after the {@link JwtAuthenticationManager}
 * successfully authenticates the principal but the subsequent authorization check — e.g. {@code hasRole("ADMIN")} on
 * {@code /api/admin/**} — fails. The access-denied path is only reachable after full JWT
 * validation; unauthenticated requests are routed to {@link JwtAuthenticationEntryPoint} instead.
 * </p>
 *
 * <p>
 * No downstream step follows: this handler terminates the exchange by writing the HTTP response
 * directly.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        log.error("Caught authorization exception with message: {}", ex.getMessage());

        exchange.getResponse().setStatusCode(FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("message", "Access denied");

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
