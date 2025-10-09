package com.sayless.auth.controller;
import com.sayless.auth.model.User;
import com.sayless.auth.repository.UserRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class UserController {
    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;   
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        Optional<User> user = repo.findById(id);
        return user.isPresent()?ResponseEntity.ok(user.get()):ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(@PathVariable String id, @RequestBody User updated) {
        Optional<User> existigOptional = repo.findById(id);
        if(existigOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existing = existigOptional.get();

        if(updated.getProfilePic() != null) existing.setProfilePic(updated.getProfilePic());
        if(updated.getBio() != null) existing.setBio(updated.getBio());

        repo.save(existing);
        return ResponseEntity.ok(existing);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String username) {
        var results = repo.findByUsernameContainingIgnoreCase(username);
        return ResponseEntity.ok(
            results.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername()
            )).toList()
        );
    }

}
