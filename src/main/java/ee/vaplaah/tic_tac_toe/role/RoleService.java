package ee.vaplaah.tic_tac_toe.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Mono<Role> findByName(String roleName) {
        return roleRepository.findByName(roleName)
            .switchIfEmpty(Mono.error(() -> new RuntimeException("No such role: " + roleName)));
    }
}