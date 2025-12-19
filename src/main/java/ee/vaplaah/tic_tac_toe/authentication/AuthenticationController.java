package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.authentication.request.LoginRequest;
import ee.vaplaah.tic_tac_toe.authentication.request.RegisterRequest;
import ee.vaplaah.tic_tac_toe.authentication.response.LoginResponse;
import ee.vaplaah.tic_tac_toe.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;

    @GetMapping("/current")
    public Authentication current(Authentication authentication) {
        log.info("Receiving the authentication object");
        return authentication;
    }

    @PostMapping("/register")
    @ResponseStatus(CREATED)
    public Mono<User> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received request to register user: {}", request);
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received request to login: {}", request);
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public Mono<LoginResponse> refresh(@Valid @RequestBody String refreshToken) {
        log.info("Received request to refresh authentication with token {}", refreshToken);
        return authenticationService.refreshToken(refreshToken);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout() {
        log.info("Received request to logout");
        return authenticationService.logout()
            .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
