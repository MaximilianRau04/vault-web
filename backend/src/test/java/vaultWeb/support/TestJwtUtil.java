package vaultWeb.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vaultWeb.models.User;

/**
 * Test-only JWT utility for generating tokens with custom expiration times.
 *
 * <p>This utility is restricted to the test profile and provides methods needed for testing JWT
 * authentication scenarios such as expired tokens.
 */
@Component
@Profile("test")
public class TestJwtUtil {

  private final SecretKey secretKey;

  public TestJwtUtil(@Value("${jwt.secret}") String secret) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * Generates a signed JWT token with custom expiration time.
   *
   * @param user the user entity containing username
   * @param expirationMillis expiration time in milliseconds from now (can be negative for expired
   *     tokens)
   * @return a signed JWT token string
   */
  public String generateTokenWithExpiration(User user, long expirationMillis) {
    return Jwts.builder()
        .setSubject(user.getUsername())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
        .signWith(secretKey)
        .compact();
  }
}
