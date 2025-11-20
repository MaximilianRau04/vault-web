package vaultWeb.services.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vaultWeb.exceptions.notfound.UserNotFoundException;
import vaultWeb.models.User;
import vaultWeb.repositories.UserRepository;
import vaultWeb.security.JwtUtil;

/**
 * Service class responsible for handling authentication and user session-related operations.
 *
 * <p>Provides functionality for:
 *
 * <ul>
 *   <li>Authenticating users with username and password.
 *   <li>Generating JWT tokens for authenticated users.
 *   <li>Retrieving the currently authenticated user from the security context.
 * </ul>
 *
 * <p>This service integrates with Spring Security's AuthenticationManager for authentication,
 * UserRepository for fetching user entities, and JwtUtil for generating JWT tokens.
 *
 * <p>Security considerations:
 *
 * <ul>
 *   <li>Passwords are never stored or transmitted in plaintext.
 *   <li>Authentication uses BCryptPasswordEncoder for secure password hashing.
 *   <li>JWT tokens are signed and include necessary claims (e.g., username, role) for stateless
 *       authentication.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  /**
   * Authenticates a user using their username and password and returns a JWT token upon successful
   * authentication.
   *
   * <p>Workflow:
   *
   * <ol>
   *   <li>The AuthenticationManager validates the username and password against the stored hash.
   *   <li>If authentication succeeds, the Authentication object is stored in the SecurityContext.
   *   <li>UserDetails are retrieved from the Authentication object, containing basic security info
   *       (username, roles).
   *   <li>The full User entity is then loaded from the database for additional details.
   *   <li>A JWT token is generated for the user, signed and valid for a specific duration.
   * </ol>
   *
   * <p>Detailed notes on {@code authenticationManager.authenticate(...)}:
   *
   * <ul>
   *   <li>Spring Security calls the UserDetailsService to fetch user info by username.
   *   <li>The provided password is compared with the stored hashed password using PasswordEncoder.
   *   <li>If the password matches, a fully authenticated Authentication object is returned.
   *   <li>If the password does not match, a BadCredentialsException is thrown.
   * </ul>
   *
   * @param username the username provided by the client
   * @param password the plaintext password provided by the client
   * @return a signed JWT token representing the authenticated user
   * @throws UserNotFoundException if the user does not exist in the database
   */
  public String login(String username, String password) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

    User user =
        userRepository
            .findByUsername(userDetails.getUsername())
            .orElseThrow(
                () -> new UserNotFoundException("User not found: " + userDetails.getUsername()));

    return jwtUtil.generateToken(user);
  }

  /**
   * Retrieves the currently authenticated user from the SecurityContext.
   *
   * <p>If no user is authenticated, this method returns {@code null}. Otherwise, it fetches the
   * full {@link User} entity from the database based on the username.
   *
   * @return the currently authenticated {@link User}, or {@code null} if no user is authenticated
   */
  public User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      return userRepository.findByUsername(userDetails.getUsername()).orElse(null);
    }

    return null;
  }
}
