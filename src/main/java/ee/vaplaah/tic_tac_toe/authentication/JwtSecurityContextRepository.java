package ee.vaplaah.tic_tac_toe.authentication;

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
 * Loads the SecurityContext from the incoming request, using the JWT.
 * This effectively acts as the "JWT Filter" for WebFlux.
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
        log.info("[JwtSecurityContextRepository] Loading SecurityContext for incoming request {}", exchange.getRequest().getURI());
        return converter.convert(exchange)
            .flatMap(this.authenticationManager::authenticate)
            .map(SecurityContextImpl::new);
    }
}