package ee.vaplaah.gomoku.configuration;

import ee.vaplaah.gomoku.role.Role;
import ee.vaplaah.gomoku.role.RoleRepository;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Idempotent seed-data loader that ensures the minimum required reference data — the
 * {@code USER} and {@code ADMIN} roles, and a default {@code admin} user — exist in MongoDB
 * before the application begins serving traffic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        createRoleIfNotExists("USER");
        createRoleIfNotExists("ADMIN");
        createAdminIfNotExists();
    }

    private void createRoleIfNotExists(String roleName) {
        roleRepository.findByName(roleName)
            .switchIfEmpty(Mono.defer(() -> {
                Role role = Role.builder()
                    .name(roleName)
                    .build();
                return roleRepository.save(role);
            }))
            .subscribe();
    }

    private void createAdminIfNotExists() {
        userRepository.findByUsername("admin")
            .switchIfEmpty(Mono.defer(() -> {
                User user = User.builder()
                    .username("admin")
                    .password("$2a$12$0xqBcM.oEaAfyw8numeax.3Q3gqTsvq12wHD36iHoFq.5og9yz022")
                    .roles(List.of("ADMIN", "USER"))
                    .build();
                return userRepository.save(user);
            }))
            .subscribe();
    }
}