package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.game.dto.GameDto;
import ee.vaplaah.tic_tac_toe.game.request.CreateGameRequest;
import ee.vaplaah.tic_tac_toe.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public Mono<GameDto> findById(String gameId) {
        return gameRepository.findById(gameId)
            .map(GameDto::from);
    }

    public Mono<GameDto> createGame(CreateGameRequest request, User user) {
        log.info("Creating new game");
        String creatorId = user.getId();
        Integer boardSize = request.getBoardSize();
        Game build = Game.builder()
            .boardSize(boardSize)
            .winningCount(request.getWinningCount())
            .board(new String[boardSize][boardSize])
            .players(List.of(creatorId))
            .build();
        return gameRepository.save(build)
            .flatMap(game -> Mono.just(GameDto.from(game)));
    }
}
