package ee.vaplaah.tic_tac_toe.authentication.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Dual-state credential carrier that flows through the entire JWT authentication pipeline,
 * starting as an unauthenticated envelope holding a raw token string, and ending as a fully
 * authenticated principal holding a {@link UserDetails} and its granted authorities.
 *
 * <p><strong>State summary:</strong>
 * <ul>
 *   <li><em>Pre-auth</em>: {@code token} = raw JWT string, {@code principal} = null,
 *       {@code isAuthenticated() = false}.</li>
 *   <li><em>Post-auth</em>: {@code token} = null, {@code principal} = loaded
 *       {@code UserDetails}, {@code isAuthenticated() = true}.</li>
 * </ul>
 * </p>
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token; // Holds the raw JWT string before authentication
    private final UserDetails principal; // Holds the UserDetails object after authentication
    private final boolean requiresNotExpired; // Indicates if the token must be not expired for the request

    /**
     * Constructor used when the token is first extracted from the request (unauthenticated).
     * @param token The raw JWT string.
     */
    public JwtAuthenticationToken(String token, boolean requiresNotExpired) {
        super(null);
        this.token = token;
        this.requiresNotExpired = requiresNotExpired;
        this.setAuthenticated(false);
        this.principal = null; // Principal is not set yet
    }

    /**
     * Constructor used after successful token verification (authenticated).
     * @param principal The fully loaded UserDetails object.
     * @param authorities The authorities/roles of the user.
     */
    public JwtAuthenticationToken(UserDetails principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.token = null; // Token is no longer needed after verification
        this.requiresNotExpired = false; // Not relevant after authentication
        this.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token; // The JWT string is the credential
    }

    @Override
    public Object getPrincipal() {
        return principal; // The authenticated UserDetails
    }

    public boolean requiresNotExpired() {
        return requiresNotExpired;
    }
}