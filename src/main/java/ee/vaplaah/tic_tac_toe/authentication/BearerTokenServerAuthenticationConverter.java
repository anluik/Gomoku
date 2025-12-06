package ee.vaplaah.tic_tac_toe.authentication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Converts the HTTP request (specifically the Authorization header) into an Authentication object.
 * For example, this app uses JwtAuthenticationToken class to represent a JWT Authentication object.
 */
@Slf4j
@Component
public class BearerTokenServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Function<String, Mono<String>> extractToken = token ->
        Mono.justOrEmpty(token.substring(BEARER_PREFIX.length()));

    /**
     * Extracts the JWT from the "Authorization: Bearer <token>" header.
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        log.info("[BearerTokenServerAuthenticationConverter] Converting incoming request to an Authentication object");
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
            .filter(header -> header.startsWith(BEARER_PREFIX))
            .flatMap(extractToken)
            .map(JwtAuthenticationToken::new);
    }
}
