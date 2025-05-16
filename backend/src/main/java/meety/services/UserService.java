package meety.services;

import meety.models.User;
import meety.models.enums.Role;
import meety.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.User);
        userRepository.save(user);
    }

    public User findByUsername(String username){
        return userRepository.findByUsername(username).orElse(null);
    }
}
