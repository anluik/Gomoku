package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.exception.ResourceNotFoundException;
import ee.vaplaah.tic_tac_toe.game.dto.GameDto;
import ee.vaplaah.tic_tac_toe.game.request.CreateGameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game")
public class GameController {

    private final GameService gameService;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<GameDto>> findGame(@PathVariable String id) {
        return gameService.findById(id)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Game with id " + id + " not found")));
    }

    @PostMapping
    public Mono<ResponseEntity<GameDto>> createGame(@Valid @RequestBody CreateGameRequest request) {
        return gameService.createGame(request)
            .map(game -> ResponseEntity.status(CREATED).body(game));
    }
}
