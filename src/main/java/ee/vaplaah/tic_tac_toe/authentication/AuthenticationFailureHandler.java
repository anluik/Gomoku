package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.configuration.filter.AuthenticationFilter;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.InvalidTokenException;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adapts authentication failure events to be handled uniformly by the {@link JwtAuthenticationEntryPoint}.
 * This implementation delegates the failure directly to the JWT entry point ensuring that both unauthenticated
 * resource access and failed login attempts return a consistent API error structure and HTTP status code.
 *
 * <p>Called by {@link AuthenticationFilter} whenever a {@link AuthenticationException} is thrown during
 * the authentication process. For example, when the JWT signature is invalid ({@link InvalidTokenException})
 * or the token has expired ({@link TokenExpiredException}).
 * In other words, the request provided a JWT, but something caused the authentication to fail (e.g. malformed token or
 * invalid password).
 * </p>
 * <p>It is NOT invoked for authorization failures (wrong role) — those go to {@link AccessDeniedHandler}.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        return jwtAuthenticationEntryPoint.commence(webFilterExchange.getExchange(), exception).then();
    }
}
