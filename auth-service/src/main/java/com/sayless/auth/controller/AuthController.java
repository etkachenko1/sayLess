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

@RestController //tells SpringBoot that this class handles HTTP requests and will return JSON and HTML
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        //takes Json request and parses it into a map
        //also shoyld have a name and birthday and about me 
        String username = body.get("username");
        String email = body.get("email");
        String password  = body.get("password");

        //check if the user already exists
        if(userRepo.existsByUsername(username)) return ResponseEntity.badRequest().body("This username is already taken.");
        if(userRepo.existsByEmail(email)) return ResponseEntity.badRequest().body("This email is already taken.");

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

        var query = userRepo.findByUsername(username);
        if(query.isEmpty()) return ResponseEntity.status(401).body("This username does not exist");
        User u = query.get();
        if(!passwordEncoder.matches(password, u.getPassword())) return ResponseEntity.status(401).body("Invalid credentials");

        String token = jwtUtil.generateToken(u.getId());
        return ResponseEntity.ok(Map.of("token", token));
        
    }
    
}
