package meety.services.auth;

import meety.models.User;
import meety.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service class that integrates the application's User entity with Spring Security.
 * <p>
 * Implements {@link UserDetailsService}, which is used by Spring Security during the authentication process.
 * This service loads user-specific data given a username.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Retrieve a {@link User} entity from the database via {@link UserRepository}.</li>
 *     <li>Convert the domain {@link User} into a Spring Security {@link UserDetails} object.</li>
 *     <li>Throw {@link UsernameNotFoundException} if the user does not exist, signaling authentication failure.</li>
 * </ul>
 * <p>
 * Detailed workflow:
 * <ol>
 *     <li>Spring Security calls {@link #loadUserByUsername(String)} with the username supplied during login.</li>
 *     <li>The method queries the {@link UserRepository} to fetch the {@link User} entity.</li>
 *     <li>If no user is found, a {@link UsernameNotFoundException} is thrown.</li>
 *     <li>If the user is found, a {@link UserDetails} object is built using:
 *         <ul>
 *             <li>username: used in the security context as the principal</li>
 *             <li>password: hashed password stored in the database (e.g., BCrypt)</li>
 *             <li>authorities: roles or permissions (can be added here if needed)</li>
 *         </ul>
 *     </li>
 *     <li>Spring Security then compares the provided plaintext password with the stored hash using the configured {@link org.springframework.security.crypto.password.PasswordEncoder}.</li>
 *     <li>If the passwords match, authentication succeeds; otherwise, it fails.</li>
 * </ol>
 * <p>
 * This design decouples the application's {@link User} entity from Spring Security's internal representation,
 * providing flexibility and security abstraction.
 */
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads the user details for Spring Security based on the given username.
     *
     * @param username The username of the user attempting to authenticate.
     * @return A {@link UserDetails} object containing the username, hashed password, and authorities.
     * @throws UsernameNotFoundException if the username does not exist in the database.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .build();
    }
}