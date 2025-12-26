package ee.vaplaah.tic_tac_toe.game_result;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GameResultRepository extends ReactiveMongoRepository<GameResult, String> {

    Mono<GameResult> findByGameId(String gameId);

    @Query("{ 'players.userId' : ?0 }")
    Flux<GameResult> findByUserId(String userId);
}
