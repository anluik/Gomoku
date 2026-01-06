package ee.vaplaah.tic_tac_toe.authentication;

import ee.vaplaah.tic_tac_toe.core.exception.authentication.InvalidTokenException;
import ee.vaplaah.tic_tac_toe.core.exception.authentication.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-seconds}")
    private Long accessTokenExpirationSeconds;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // --- Token Generation ---

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", "")).toList());

        return createToken(claims, userDetails.getUsername(), accessTokenExpirationSeconds);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .claims(claims)
            .subject(subject) // The user's ID/username
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationSeconds * 1000))
            .signWith(getSigningKey(), Jwts.SIG.HS512)
            .compact();
    }

    // --- Token Validation and Extraction ---

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException(ex.getClaims());
        } catch (Exception ex) {
            throw new InvalidTokenException();
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}