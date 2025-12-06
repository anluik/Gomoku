package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.exception.ResourceNotFoundException;
import ee.vaplaah.tic_tac_toe.game.dto.GameDto;
import ee.vaplaah.tic_tac_toe.game.request.CreateGameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game")
public class GameController {

    private final GameService gameService;

    @GetMapping("/{id}")
    public Mono<GameDto> findGame(@PathVariable String id) {
        log.info("[GameController] Finding game by id: {}", id);
        return gameService.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Game with id " + id + " not found")));
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @PreAuthorize("hasRole('USER')")
    public Mono<GameDto> createGame(@Valid @RequestBody CreateGameRequest request) {
        log.info("[GameController] Creating a game by id");
        return gameService.createGame(request);
    }
}
