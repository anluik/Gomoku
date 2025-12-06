package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.exception.authentication.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Manages the actual authentication process for a JWT.
 * It verifies the token and loads the associated user details.
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
        log.info("[JwtAuthenticationManager] Authenticating user {}", authentication.getName());
        String authToken = authentication.getCredentials().toString();

        String username = jwtService.extractUsername(authToken);
        if (username != null) {
            return userDetailsService.findByUsername(username)
                .flatMap(userDetails -> {
                    if (jwtService.validateAccessToken(authToken, userDetails)) {
                        return Mono.just(
                            new JwtAuthenticationToken(userDetails, userDetails.getAuthorities())
                        );
                    } else {
                        return Mono.error(InvalidTokenException::new);
                    }
                });
        }

        // If no username could be extracted from the token
        return Mono.error(InvalidTokenException::new);
    }
}