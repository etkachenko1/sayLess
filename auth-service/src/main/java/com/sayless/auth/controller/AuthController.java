// REST API aka /register /login
package com.sayless.auth.controller;

import com.sayless.auth.model.User;
import com.sayless.auth.repository.UserRepository;
import com.sayless.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; //alllows to build HTTP responses
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; //to hash passwords
import java.util.Map; //to read JSON request bodies as key-value pairs
import java.util.Set;
import java.util.regex.Pattern;

@RestController //tells SpringBoot that this class handles HTTP requests and will return JSON and HTML
@RequestMapping("/auth")
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password1", "password123", "qwerty123", "welcome1", "welcome123", "letmein1",
        "letmein123", "admin1234", "iloveyou1", "monkey123", "football1", "baseball1", "dragon123",
        "master123", "sunshine1", "princess1", "trustno1", "superman1", "changeme1",
        "passw0rd", "abc123456", "whatever1", "starwars1", "shadow123", "michael1"
    );

    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String DUMMY_HASH_FOR_TIMING_SAFETY = new BCryptPasswordEncoder().encode("dummy-password-for-timing-safety-check");

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        //takes Json request and parses it into a map
        String username = body.get("username");
        String email = body.get("email");
        String password  = body.get("password");

        if (username == null || username.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        if (!EMAIL_PATTERN.matcher(email).matches()) return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid email address"));
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least " + MIN_PASSWORD_LENGTH + " characters"));
        if (!password.chars().anyMatch(Character::isUpperCase) || !password.chars().anyMatch(Character::isDigit)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must contain at least one uppercase letter and one number"));
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) return ResponseEntity.badRequest().body(Map.of("error", "This password is too common. Please choose a different one"));

        //check if the user already exists
        if(userRepo.existsByUsername(username)) return ResponseEntity.badRequest().body(Map.of("error", "This username is already taken."));
        if(userRepo.existsByEmail(email)) return ResponseEntity.badRequest().body(Map.of("error", "This email is already taken."));
        //Proceed if not:
        User u = new User(username, email, passwordEncoder.encode(password));
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "register"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login (@RequestBody Map<String,String> body) {
        //check if the user exist
        //if doesn't throw erre
        //if does and the password matches, then create session jwt token
        String username = body.get("username");
        String password  = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank())
            return ResponseEntity.status(401).body(Map.of("error", "Username and password are required"));

        User u = userRepo.findByUsername(username).orElse(null);
        String hashToCheck = (u != null) ? u.getPassword() : DUMMY_HASH_FOR_TIMING_SAFETY;
        boolean passwordMatches = passwordEncoder.matches(password, hashToCheck);
        if (u == null || !passwordMatches) return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));

        String token = jwtUtil.generateToken(u.getId());
        return ResponseEntity.ok(Map.of("token", token));
        
    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        boolean exists = userRepo.existsByUsername(username);
        return ResponseEntity.ok(Map.of("available", !exists));
    }
    
}
