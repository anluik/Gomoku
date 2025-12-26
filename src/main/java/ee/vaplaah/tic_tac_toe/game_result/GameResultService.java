package ee.vaplaah.tic_tac_toe.game_result;

import ee.vaplaah.tic_tac_toe.game_result.dto.GameResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameResultService {

    private final GameResultRepository gameResultRepository;

    public Mono<GameResultDto> findByGameId(String gameId) {
        return gameResultRepository.findByGameId(gameId)
            .flatMap(gameResult -> Mono.just(GameResultDto.from(gameResult)));
    }

    // TODO: implement pagination
    public Flux<GameResultDto> findByUserId(String gameId) {
        return gameResultRepository.findByUserId(gameId)
            .flatMap(gameResult -> Mono.just(GameResultDto.from(gameResult)));
    }
}