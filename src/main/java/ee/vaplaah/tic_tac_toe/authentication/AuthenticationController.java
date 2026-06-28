package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.authentication.request.LoginRequest;
import ee.vaplaah.tic_tac_toe.authentication.request.RefreshRequest;
import ee.vaplaah.tic_tac_toe.authentication.request.RegisterRequest;
import ee.vaplaah.tic_tac_toe.authentication.response.AuthenticationResponse;
import ee.vaplaah.tic_tac_toe.authentication.response.LoginResponse;
import ee.vaplaah.tic_tac_toe.authentication.response.RefreshResponse;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.dto.AuthenticatedUserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * REST controller that exposes the public authentication endpoints under {@code /api/auth}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;

    @Value("${jwt.access-token-expiration-seconds}")
    private Long accessTokenExpirationSeconds;

    /**
     * Authenticated endpoint to get the authentication status of the current user.
     * @param authentication the authentication object provided by Spring Security.
     * @return a Mono with authentication details.
     * Never returns unauthenticated response because to reach here the user must be authenticated.
     */
    @GetMapping("/status")
    public Mono<AuthenticationResponse> getAuthStatus(Authentication authentication) {
        log.info("Received request to get auth status");
        User user = (User) authentication.getPrincipal();
        log.info("Authenticated user: {}", user);
        return Mono.just(AuthenticationResponse.builder()
            .authenticated(true)
            .user(AuthenticatedUserDto.from(user))
            .expiresInSeconds(accessTokenExpirationSeconds)
            .build());
    }

    /**
     * Unauthenticated endpoint to register a new user.
     * @param request - the registration request containing username and password.
     * @return a Mono with the created User.
     */
    @PostMapping("/register")
    @ResponseStatus(CREATED)
    public Mono<User> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received request to register user: {}", request);
        // TODO: return some DTO
        return authenticationService.register(request);
    }

    /**
     * Unauthenticated endpoint to log in a user.
     * @param request - the login request containing username and password.
     * @return a Mono with the login response containing access tokens.
     */
    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received request to login: {}", request);
        return authenticationService.login(request);
    }

    /**
     * Authenticated endpoint to refresh the access token using a refresh token.
     * @param refreshRequest - the refresh request containing the refresh token.
     * @return a Mono with the refresh response containing new access tokens.
     */
    @PostMapping("/refresh")
    public Mono<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        log.info("Received request to refresh authentication with token {}", refreshRequest.getRefreshToken());
        return authenticationService.refreshToken(refreshRequest.getRefreshToken());
    }

    /**
     * Authenticated endpoint to log out the current user.
     * @return a Mono signaling completion of the logout process.
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout() {
        log.info("Received request to logout");
        return authenticationService.logout()
            .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
