package ee.vaplaah.gomoku.authentication;

import ee.vaplaah.gomoku.core.exception.authentication.InvalidCredentialsException;
import ee.vaplaah.gomoku.role.RoleRepository;
import ee.vaplaah.gomoku.user.User;
import ee.vaplaah.gomoku.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security bridge that loads a fully hydrated {@link UserDetails}
 * (including role-based {@link GrantedAuthority} objects)
 * from MongoDB, given a username string extracted from a validated JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(InvalidCredentialsException::new))
            .flatMap(user -> {
                List<String> userRoles = user.getRoles();
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