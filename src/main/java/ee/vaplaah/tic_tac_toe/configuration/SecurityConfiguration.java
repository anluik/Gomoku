package ee.vaplaah.tic_tac_toe.configuration;

import ee.vaplaah.tic_tac_toe.authentication.AccessDeniedHandler;
import ee.vaplaah.tic_tac_toe.authentication.JwtAuthenticationEntryPoint;
import ee.vaplaah.tic_tac_toe.configuration.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationFilter authenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .pathMatchers("/api/v1/**").hasRole("USER")
                .anyExchange().authenticated())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                // "entry point" into the process of challenging the client for credentials after AuthenticationException
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                // handle AccessDeniedException when user is authenticated but not authorized
                .accessDeniedHandler(accessDeniedHandler)
            )
            .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
