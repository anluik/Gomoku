package ee.vaplaah.gomoku.utils;

import ee.vaplaah.gomoku.user.User;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

@UtilityClass
public class SecurityUtils {

    public static Mono<User> getUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> {
                Object principal = context.getAuthentication().getPrincipal();
                if (principal instanceof UserDetails) {
                    return (User) principal;
                }
                return null;
            })
            .switchIfEmpty(Mono.empty());
    }
}
