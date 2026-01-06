package ee.vaplaah.tic_tac_toe.authentication.token.refresh_token;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshToken, String> {
    /**
     * Finds a token by its string value.
     */
    Mono<RefreshToken> findByToken(String token);

    Flux<RefreshToken> findAllByUserId(String userId);

    /**
     * Deletes a token to revoke a user's session.
     */
    Mono<Void> deleteByToken(String token);
}
