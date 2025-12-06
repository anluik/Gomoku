package ee.vaplaah.tic_tac_toe.configuration.filter;

import ee.vaplaah.tic_tac_toe.authentication.BearerTokenServerAuthenticationConverter;
import ee.vaplaah.tic_tac_toe.authentication.AuthenticationFailureHandler;
import ee.vaplaah.tic_tac_toe.authentication.JwtAuthenticationManager;
import ee.vaplaah.tic_tac_toe.authentication.JwtSecurityContextRepository;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AuthenticationWebFilter {

    public AuthenticationFilter(
        JwtAuthenticationManager authenticationManager,
        BearerTokenServerAuthenticationConverter converter,
        AuthenticationFailureHandler failureHandler,
        JwtSecurityContextRepository securityContextRepository
    ) {
        super(authenticationManager);
        this.setServerAuthenticationConverter(converter); // acquires token from request
        this.setAuthenticationFailureHandler(failureHandler); // handles exceptions during authentication
        this.setSecurityContextRepository(securityContextRepository);  // loads SecurityContext from the request
    }
}
