package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.exception.authentication.InvalidCredentialsException;
import ee.vaplaah.tic_tac_toe.role.RoleRepository;
import ee.vaplaah.tic_tac_toe.user.User;
import ee.vaplaah.tic_tac_toe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.info("[UserDetailsService] Loading UserDetails for username {}", username);
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(InvalidCredentialsException::new))
            .flatMap(user -> {
                List<String> userRoles = user.getRoles();
                log.info("[UserDetailsService] User '{}' has roles {}", username, userRoles);
                return roleRepository.findAllByNameIn(userRoles)
                    .collectList()
                    .map(roles -> {
                        roles.forEach(role -> role.setName("ROLE_" + role.getName()));
                        User userDetails = new User(
                            user.getUsername(),
                            user.getPassword(),
                            userRoles
                        ) {
                            @Override
                            public Collection<? extends GrantedAuthority> getAuthorities() {
                                return roles;
                            }
                        };
                        userDetails.setId(user.getId());
                        return userDetails;
                    });
            })
            .cast(UserDetails.class);
    }
}