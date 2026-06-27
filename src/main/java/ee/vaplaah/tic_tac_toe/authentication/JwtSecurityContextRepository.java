package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.authentication.token.JwtAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>In a stateless JWT architecture running on WebFlux, there is no session —
 * the security context must be reconstructed from scratch on every request. This class is
 * exactly this: custom context loading from the exchange.
 * </p>

 *
 * <p>Context construction process:
 * <ul>
 *   <li>{@link BearerTokenServerAuthenticationConverter} — called first in {@code load()} to
 *       extract the Bearer token and produce a pre-auth {@link JwtAuthenticationToken}.</li>
 *   <li>{@link ReactiveAuthenticationManager} ({@link JwtAuthenticationManager}) — called
 *       second to validate the token and return the authenticated principal.</li>
 *   <li>{@link org.springframework.security.core.context.SecurityContextImpl} — wraps the
 *       authenticated token; the resulting {@code Mono<SecurityContext>} is consumed by the
 *       framework to populate
 *       {@link org.springframework.security.core.context.ReactiveSecurityContextHolder} for
 *       downstream use in controllers and services.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecurityContextRepository implements ServerSecurityContextRepository {

    private final ReactiveAuthenticationManager authenticationManager;
    private final BearerTokenServerAuthenticationConverter converter;

    /**
     * This method is only needed if you were saving the security context.
     * Since we are stateless, this method does nothing.
     */
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    /**
     * Loads the SecurityContext (the authenticated user) from the HTTP request.
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return converter.convert(exchange)
            .flatMap(this.authenticationManager::authenticate)
            .map(SecurityContextImpl::new);
    }
}