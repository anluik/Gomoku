package ee.vaplaah.gomoku.authentication;

import ee.vaplaah.gomoku.authentication.token.JwtAuthenticationToken;
import ee.vaplaah.gomoku.core.exception.authentication.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Second stage of the JWT authentication pipeline: validates the raw JWT string received
 * from {@link BearerTokenServerAuthenticationConverter}, loads the associated user from
 * MongoDB, and returns a fully authenticated {@link JwtAuthenticationToken}.
 *
 * <p>This class is responsible for cryptographic validation (signature, expiry) and
 * resolving the username to a live {@link UserDetails} object. Separating extraction
 * from validation keeps each class focused on a single concern.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Processes the unauthenticated token.
     * @param authentication The JwtAuthenticationToken containing the raw JWT string.
     * @return Mono emitting the fully authenticated token or an error.
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            boolean isTokenNotExpiredRequired = jwtToken.requiresNotExpired();
            return Mono.fromCallable(() -> jwtService.extractUsername(authToken))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(TokenExpiredException.class, ex -> {
                    if (isTokenNotExpiredRequired) {
                        return Mono.error(ex);
                    }
                    // Subject contains the username from the token
                    return Mono.just(ex.getClaims().getSubject());
                })
                .flatMap(userDetailsService::findByUsername)
                .map(userDetails -> new JwtAuthenticationToken(
                    userDetails,
                    userDetails.getAuthorities()
                ));
        }

        return Mono.fromCallable(() -> jwtService.extractUsername(authToken))
            .subscribeOn(Schedulers.boundedElastic()) // JWT parsing is CPU intensive
            .flatMap(userDetailsService::findByUsername)
            .flatMap(userDetails -> Mono.just(
                new JwtAuthenticationToken(userDetails, userDetails.getAuthorities())
            ));
    }
}