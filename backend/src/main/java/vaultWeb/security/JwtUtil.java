package vaultWeb.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import vaultWeb.models.User;

/**
 * Utility class for creating and parsing JSON Web Tokens (JWT). A JWT is a compact, URL-safe token
 * format consisting of three parts: header, payload, and signature.
 *
 * <ul>
 *   <li><b>Header:</b> contains metadata about the token, such as the signing algorithm (e.g.,
 *       HS256) and token type.
 *   <li><b>Payload:</b> contains <b>claims</b> — pieces of information about the user or the token
 *       itself.
 *   <li><b>Signature:</b> cryptographic signature to ensure token integrity and authenticity.
 * </ul>
 *
 * <b>Claims</b> are key-value pairs embedded inside the JWT payload that provide data such as:
 *
 * <ul>
 *   <li><i>Registered claims</i> like <code>sub</code> (subject, often the username), <code>iat
 *       </code> (issued at), and <code>exp</code> (expiration time).
 *   <li><i>Public claims</i> which can be custom, e.g. user roles, email, etc.
 *   <li><i>Private claims</i> defined by your application for specific needs.
 * </ul>
 *
 * <p>In this class, the "role" claim is a custom public claim used to store the user's role for
 * authorization purposes.
 *
 * <p>The token is cryptographically signed using the secret key to ensure its integrity and
 * authenticity.
 */
@Component
public class JwtUtil {

  /** Secret key used for signing the JWT. Generated using HS256 (HMAC with SHA-256) algorithm. */
  private final SecretKey SECRET_KEY;

  private final long EXPIRATION_TIME = 1000 * 60 * 60;

  public JwtUtil(@Value("${jwt.secret}") String secret) {
    this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * Generates a signed JWT token for the given user. The token is signed with the {@link
   * #SECRET_KEY} using HS256.
   *
   * @param user the user entity containing username and role
   * @return a signed JWT token string
   */
  public String generateToken(User user) {
    return Jwts.builder()
        .setSubject(user.getUsername()) // set "sub" claim
        .setIssuedAt(new Date()) // current time as issue date
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // expiry time
        .signWith(SECRET_KEY) // sign token with secret key
        .compact(); // build token string
  }

  /**
   * Extracts the username (subject) from the provided JWT token.
   *
   * <p>This method also validates the token's signature using the {@link #SECRET_KEY}. If the token
   * is invalid or expired, parsing will throw an exception.
   *
   * @param token the JWT token string
   * @return the username (subject) embedded in the token
   * @throws io.jsonwebtoken.JwtException if token parsing or validation fails
   */
  public String extractUsername(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY) // set key to verify signature
        .build()
        .parseClaimsJws(token) // parse and validate token
        .getBody()
        .getSubject(); // extract "sub" claim (username)
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }

  public Authentication getAuthentication(String token) {
    String username = extractUsername(token);
    return new UsernamePasswordAuthenticationToken(
        username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
