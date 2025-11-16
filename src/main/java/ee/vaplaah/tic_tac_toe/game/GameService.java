package ee.vaplaah.tic_tac_toe.game;

import ee.vaplaah.tic_tac_toe.exception.InvalidResourceIdException;
import ee.vaplaah.tic_tac_toe.game.dto.GameDto;
import ee.vaplaah.tic_tac_toe.game.request.CreateGameRequest;
import ee.vaplaah.tic_tac_toe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public Mono<GameDto> findById(String gameId) {
        return gameRepository.findById(gameId)
            .map(GameDto::from);
    }

    public Mono<GameDto> createGame(CreateGameRequest request) {
        String creatorId = request.getCreatorId();
        return userRepository.findById(creatorId) // 1. Try to fetch the User (Mono<User>)
            .switchIfEmpty(Mono.error(new InvalidResourceIdException("User with id " + creatorId + " does not exist")))
            .map(existingUser -> {
                Integer boardSize = request.getBoardSize();
                return Game.builder()
                    .boardSize(boardSize)
                    .board(new String[boardSize][boardSize])
                    .players(List.of(creatorId))
                    .build();
            })
            .flatMap(gameRepository::save)
            .map(GameDto::from);
    }
}
