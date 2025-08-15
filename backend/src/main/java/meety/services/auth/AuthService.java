package meety.services.auth;

import meety.exceptions.notfound.UserNotFoundException;
import meety.models.User;
import meety.repositories.UserRepository;
import meety.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for handling authentication and user session-related operations.
 * <p>
 * Provides functionality for:
 * <ul>
 *     <li>Authenticating users with username and password.</li>
 *     <li>Generating JWT tokens for authenticated users.</li>
 *     <li>Retrieving the currently authenticated user from the security context.</li>
 * </ul>
 * <p>
 * This service integrates with Spring Security's AuthenticationManager for authentication,
 * UserRepository for fetching user entities, and JwtUtil for generating JWT tokens.
 * <p>
 * Security considerations:
 * <ul>
 *     <li>Passwords are never stored or transmitted in plaintext.</li>
 *     <li>Authentication uses BCryptPasswordEncoder for secure password hashing.</li>
 *     <li>JWT tokens are signed and include necessary claims (e.g., username, role) for stateless authentication.</li>
 * </ul>
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * Authenticates a user using their username and password and returns a JWT token upon successful authentication.
     * <p>
     * Workflow:
     * <ol>
     *     <li>The AuthenticationManager validates the username and password against the stored hash.</li>
     *     <li>If authentication succeeds, the Authentication object is stored in the SecurityContext.</li>
     *     <li>UserDetails are retrieved from the Authentication object, containing basic security info (username, roles).</li>
     *     <li>The full User entity is then loaded from the database for additional details.</li>
     *     <li>A JWT token is generated for the user, signed and valid for a specific duration.</li>
     * </ol>
     * <p>
     * Detailed notes on {@code authenticationManager.authenticate(...)}:
     * <ul>
     *     <li>Spring Security calls the UserDetailsService to fetch user info by username.</li>
     *     <li>The provided password is compared with the stored hashed password using PasswordEncoder.</li>
     *     <li>If the password matches, a fully authenticated Authentication object is returned.</li>
     *     <li>If the password does not match, a BadCredentialsException is thrown.</li>
     * </ul>
     *
     * @param username the username provided by the client
     * @param password the plaintext password provided by the client
     * @return a signed JWT token representing the authenticated user
     * @throws UserNotFoundException if the user does not exist in the database
     */
    public String login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userDetails.getUsername()));

        return jwtUtil.generateToken(user);
    }

    /**
     * Retrieves the currently authenticated user from the SecurityContext.
     * <p>
     * If no user is authenticated, this method returns {@code null}.
     * Otherwise, it fetches the full {@link User} entity from the database based on the username.
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
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
        }

        return null;
    }
}