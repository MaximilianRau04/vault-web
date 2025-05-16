package meety.controllers;

import meety.dtos.UserDto;
import meety.models.User;
import meety.services.UserService;
import meety.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user){
        userService.registerUser(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto user){
        try {
            String token = authService.login(user.getUsername(), user.getPassword());
            return ResponseEntity.ok(token);
        } catch (Exception e){
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

}
