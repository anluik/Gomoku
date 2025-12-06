package ee.vaplaah.tic_tac_toe.configuration;

import ee.vaplaah.tic_tac_toe.role.Role;
import ee.vaplaah.tic_tac_toe.role.RoleRepository;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

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