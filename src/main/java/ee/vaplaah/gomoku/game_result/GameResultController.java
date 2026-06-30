package ee.vaplaah.gomoku.game_result;

import ee.vaplaah.gomoku.core.exception.ResourceNotFoundException;
import ee.vaplaah.gomoku.game_result.dto.GameResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game-result")
public class GameResultController {

    private final GameResultService gameResultService;

    @GetMapping("/game/{gameId}")
    public Mono<GameResultDto> findGameResultByGameId(@PathVariable String gameId) {
        return gameResultService.findByGameId(gameId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Result for game with id " + gameId + " not found")));
    }

    @GetMapping("/user/{userId}")
    public Flux<GameResultDto> findGameResultsForUser(@PathVariable String userId) {
        return gameResultService.findByUserId(userId);
    }
}
