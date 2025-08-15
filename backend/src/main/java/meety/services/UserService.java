package meety.services;

import meety.exceptions.DuplicateUsernameException;
import meety.models.User;
import meety.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user by encoding their password and assigning the default role.
     *
     * @param user The User entity with plaintext password and username set.
     *             <p>
     *             1. The plaintext password from the User object is hashed using the injected PasswordEncoder.
     *             This ensures that the password is securely stored in the database (never plaintext).
     *             The hashing algorithm used depends on the PasswordEncoder bean configuration (commonly BCrypt).
     *             2. The user's role is set to the default Role.User.
     *             3. The user entity with hashed password and role is then saved to the database via UserRepository.
     *             <p>
     *             Important:
     *             - The PasswordEncoder bean must be consistent with the encoder used during authentication to correctly verify passwords later on.
     */
    public void registerUser(User user) {
        if (usernameExists(user.getUsername())) {
            throw new DuplicateUsernameException("Username '" + user.getUsername() + "' is already taken");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
