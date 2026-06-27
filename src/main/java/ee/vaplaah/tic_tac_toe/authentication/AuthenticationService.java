package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.authentication.response.RefreshResponse;
import ee.vaplaah.tic_tac_toe.authentication.token.refresh_token.RefreshTokenService;
import ee.vaplaah.tic_tac_toe.authentication.request.LoginRequest;
import ee.vaplaah.tic_tac_toe.authentication.request.RegisterRequest;
import ee.vaplaah.tic_tac_toe.authentication.response.LoginResponse;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.InvalidCredentialsException;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.InvalidTokenException;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.UsernameTakenException;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserRepository;
import ee.vaplaah.tic_tac_toe.user.dto.AuthenticatedUserDto;
import ee.vaplaah.tic_tac_toe.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Core authentication service that owns all identity lifecycle operations: user registration,
 * credential-based login, access-token refresh, and logout.
 */
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
        log.info("Register request: {}", request);
        return userRepository.findByUsername(request.getUsername())
            .flatMap(existingUser -> Mono.<User>error(new UsernameTakenException()))
            .switchIfEmpty(createNewUser(request));
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        return userDetailsService.findByUsername(request.getUsername())
            .cast(User.class)
            .flatMap(user -> {
                if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    String accessToken = jwtService.generateAccessToken(user);
                    return refreshTokenService.createRefreshToken(user)
                        .map(refreshToken ->
                            LoginResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken.getToken())
                                .expiresInSeconds(accessTokenExpirationSeconds)
                                .build()
                        );
                } else {
                    return Mono.error(InvalidCredentialsException::new);
                }
            })
            .switchIfEmpty(Mono.error(InvalidCredentialsException::new));
    }

    public Mono<RefreshResponse> refreshToken(String refreshTokenString) {
        return refreshTokenService.findByToken(refreshTokenString)
            .flatMap(refreshTokenService::verifyExpiration) // verify token exists and is not expired
            .flatMap(token ->
                userRepository.findById(token.getUserId())
                    .flatMap(user -> {
                        // delete the old refresh token
                        return refreshTokenService.deleteByToken(token.getToken())
                            .then(Mono.just(user));
                    })
            )
            .flatMap(user -> {
                String newAccessToken = jwtService.generateAccessToken(user);
                return refreshTokenService.createRefreshToken(user)
                    .map(newRefreshToken ->
                        RefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .expiresInSeconds(accessTokenExpirationSeconds)
                            .user(AuthenticatedUserDto.from(user))
                            .build()
                    );
            })
            .switchIfEmpty(Mono.error(InvalidTokenException::new));
    }

    public Mono<Void> logout() {
        return SecurityUtils.getUser().flatMap(user -> {
            // TODO - implement blacklisting of access tokens
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