package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.authentication.token.JwtAuthenticationToken;
import ee.vaplaah.tic_tac_toe.configuration.filter.AuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * First stage of the JWT authentication pipeline: converts the HTTP request (specifically the Authorization header)
 * into an Authentication object. This app uses {@link JwtAuthenticationToken} class to represent a JWT Authentication
 * object.
 *
 * <p>Authentication filter requires a dedicated converter to extract credentials from the request before delegating
 * to the authentication manager.
 * </p>
 *
 * <p><strong>Lifecycle &amp; Flow Triggers:</strong> Called on every request that passes through
 * {@link AuthenticationFilter} except those which have been excluded from authentication process.
 * If the header is absent or not prefixed with {@code "Bearer "}, {@code Mono.empty()} is
 * returned and the chain continues without authentication.
 * </p>
 *
 * <p>For requests to {@code /api/auth/refresh}, the produced token is stamped with {@code requiresNotExpired = false}.
 * This flag is read by {@link JwtAuthenticationManager} to skip the expiry check and allow an expired access JWT to
 * be parsed — necessary so the client can prove identity when obtaining a new token pair.
 * </p>
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
        String requestPath = exchange.getRequest().getPath().value();
        Mono<String> authToken = Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
            .filter(header -> header.startsWith(BEARER_PREFIX))
            .flatMap(extractToken);
        boolean isRefreshTokenRequest = "/api/auth/refresh".equals(requestPath);
        return authToken
            .map(token -> new JwtAuthenticationToken(token, !isRefreshTokenRequest));
    }
}
