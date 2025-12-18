package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.authentication.refresh_token.RefreshTokenService;
import ee.vaplaah.tic_tac_toe.authentication.request.LoginRequest;
import ee.vaplaah.tic_tac_toe.authentication.request.RegisterRequest;
import ee.vaplaah.tic_tac_toe.authentication.response.LoginResponse;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.InvalidCredentialsException;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.InvalidTokenException;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.UsernameTakenException;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserRepository;
import ee.vaplaah.tic_tac_toe.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.access-token-expiration-seconds}")
    private Long accessTokenExpirationSeconds;

    public Mono<User> register(RegisterRequest request) {
        log.info("[AuthenticationService] Register request: {}", request);
        return userRepository.findByUsername(request.getUsername())
            .flatMap(existingUser -> Mono.<User>error(new UsernameTakenException()))
            .switchIfEmpty(createNewUser(request));
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        log.info("[AuthenticationService] Login request: {}", request);
        return userDetailsService.findByUsername(request.getUsername())
            .cast(User.class)
            .flatMap(user -> {
                log.info("[AuthenticationService] Found user by username: {}", request.getUsername());
                if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    String accessToken = jwtService.generateAccessToken(user);
                    return refreshTokenService.createRefreshToken(user)
                        .map(refreshToken ->
                            LoginResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .expiresIn(accessTokenExpirationSeconds)
                                .build()
                        );
                } else {
                    log.info("[AuthenticationService] Incorrect password provided for username: {}", request.getUsername());
                    return Mono.error(InvalidCredentialsException::new);
                }
            })
            .switchIfEmpty(Mono.error(InvalidCredentialsException::new));
    }

    public Mono<LoginResponse> refreshToken(String refreshTokenString) {
        log.info("[AuthenticationService] Refreshing access token");
        return refreshTokenService.findByToken(refreshTokenString)
            .flatMap(refreshTokenService::verifyExpiration) // 1. Verify token exists and is not expired
            .flatMap(token ->
                // 2. Fetch the user linked to the Refresh Token
                userRepository.findById(token.getUserId())
                    .flatMap(user -> {
                        // 3. Delete the old Refresh Token (Ensures the old one can't be reused)
                        return refreshTokenService.deleteByToken(token.getToken())
                            .then(Mono.just(user));
                    })
            )
            .flatMap(user -> {
                // 4. Issue new Access and Refresh Tokens
                String newAccessToken = jwtService.generateAccessToken(user);
                return refreshTokenService.createRefreshToken(user)
                    .map(newRefreshToken ->
                        LoginResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .expiresIn(accessTokenExpirationSeconds)
                            .build()
                    );
            })
            .switchIfEmpty(Mono.error(InvalidTokenException::new));
    }

    public Mono<Void> logout() {
        return SecurityUtils.getUser().flatMap(user -> {
            log.info("[AuthenticationService] Logging out user {}", user.getId());
            // TODO - implement blacklisting of access tokens?
            return refreshTokenService.findByUserId(user.getId())
                .flatMap(refreshToken -> refreshTokenService.deleteByToken(refreshToken.getToken()))
                .then();
        });
    }

    private Mono<User> createNewUser(RegisterRequest request) {
        User newUser = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .roles(Collections.singletonList("USER"))
            .build();
        return userRepository.save(newUser);
    }
}