package ee.vaplaah.tic_tac_toe.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(auth -> {
                auth.anyExchange().permitAll();
//                auth.pathMatchers("/api/v1").permitAll();
//                auth.pathMatchers("/admin/**").hasRole("ADMIN");
//                auth.pathMatchers("/user/**").hasRole("USER");
//                auth.anyExchange().authenticated();
            })
            .formLogin(form -> form.loginPage("/login"));

        return http.build();
    }
}
