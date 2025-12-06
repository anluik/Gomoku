package ee.vaplaah.tic_tac_toe.authentication.refresh_token;

import ee.vaplaah.tic_tac_toe.exception.authentication.TokenExpiredException;
import ee.vaplaah.tic_tac_toe.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-seconds}")
    private Long refreshTokenExpirationSeconds;

    public Mono<RefreshToken> createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
            .userId(user.getId())
            .token(UUID.randomUUID().toString())
            .expiryDate(Instant.now().plusSeconds(refreshTokenExpirationSeconds))
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Mono<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public Flux<RefreshToken> findByUserId(String userId) {
        return refreshTokenRepository.findAllByUserId(userId);
    }

    public Mono<RefreshToken> verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            return refreshTokenRepository.delete(token)
                .then(Mono.error(TokenExpiredException::new));
        }
        return Mono.just(token);
    }

    public Mono<Void> deleteByToken(String token) {
        return refreshTokenRepository.deleteByToken(token);
    }
}