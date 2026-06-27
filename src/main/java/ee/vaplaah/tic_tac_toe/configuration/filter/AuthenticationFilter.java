package ee.vaplaah.tic_tac_toe.configuration.filter;

import ee.vaplaah.tic_tac_toe.authentication.BearerTokenServerAuthenticationConverter;
import ee.vaplaah.tic_tac_toe.authentication.AuthenticationFailureHandler;
import ee.vaplaah.tic_tac_toe.authentication.JwtAuthenticationEntryPoint;
import ee.vaplaah.tic_tac_toe.authentication.JwtAuthenticationManager;
import ee.vaplaah.tic_tac_toe.authentication.JwtSecurityContextRepository;
import ee.vaplaah.tic_tac_toe.authentication.token.JwtAuthenticationToken;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.stereotype.Component;

/**
 * Pre-configured Spring Security {@link AuthenticationWebFilter} that wires together all JWT
 * authentication components and excludes public endpoints from token enforcement.
 *
 * <p>
 * {@link AuthenticationWebFilter} is a powerful but
 * blank-slate filter: it needs a converter, a failure handler, and a security context
 * repository before it does anything useful. It would be possible to inject all of them into
 * {@link ee.vaplaah.tic_tac_toe.configuration.SecurityConfiguration} and configure them there directly, but
 * this class keeps the wiring logic in one place and only this bean needs to be injected.
 * </p>
 *
 * <ol>
 *   <li>The {@link NegatedServerWebExchangeMatcher} checks whether the path is
 *       {@code /api/auth/register} or {@code /api/auth/login}; if so, the filter skips
 *       authentication entirely.</li>
 *   <li>{@link BearerTokenServerAuthenticationConverter} extracts the Bearer token from the
 *       Authorization header and produces a pre-auth {@link JwtAuthenticationToken}.</li>
 *   <li>{@link JwtSecurityContextRepository} (which internally calls
 *       {@link JwtAuthenticationManager}) validates the JWT and populates the SecurityContext.</li>
 *   <li>On failure, {@link AuthenticationFailureHandler} delegates to
 *       {@link JwtAuthenticationEntryPoint} to write a
 *       JSON 401 response.</li>
 * </ol>
 * </p>
 */
@Component
public class AuthenticationFilter extends AuthenticationWebFilter {

    public AuthenticationFilter(
        JwtAuthenticationManager authenticationManager,
        BearerTokenServerAuthenticationConverter converter,
        AuthenticationFailureHandler failureHandler,
        JwtSecurityContextRepository securityContextRepository
    ) {
        super(authenticationManager);
        this.setServerAuthenticationConverter(converter); // acquires token from request
        this.setAuthenticationFailureHandler(failureHandler); // handles exceptions during authentication
        this.setSecurityContextRepository(securityContextRepository);  // loads SecurityContext from the request

        // do not try to authenticate requests for the given paths
        NegatedServerWebExchangeMatcher matcher = new NegatedServerWebExchangeMatcher(
            ServerWebExchangeMatchers.pathMatchers("/api/auth/register", "/api/auth/login"));
        this.setRequiresAuthenticationMatcher(matcher);
    }
}
