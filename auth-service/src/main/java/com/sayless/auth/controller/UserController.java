package com.sayless.auth.controller;
import com.sayless.auth.dto.UpdateProfileDto;
import com.sayless.auth.model.User;
import com.sayless.auth.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final int BIO_MAX_LENGTH = 500;
    private static final int PROFILE_PIC_MAX_LENGTH = 2048;

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    //helper to extract userId stored as principal in JwtAuthFilter from token
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        Optional<User> user = repo.findById(id);
        return user.isPresent()?ResponseEntity.ok(user.get()):ResponseEntity.notFound().build();
    }

    //callers may only ever update their own profile - id comes from the token, never from the path
    @PutMapping("/me")
    public ResponseEntity<?> updateUserProfile(@RequestBody UpdateProfileDto dto, Authentication auth) {
        String me = uid(auth);
        Optional<User> existingOptional = repo.findById(me);
        if(existingOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (dto.bio() != null && dto.bio().length() > BIO_MAX_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bio must be " + BIO_MAX_LENGTH + " characters or fewer"));
        }
        if (dto.profilePic() != null && !isValidProfilePicUrl(dto.profilePic())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Profile picture must be a valid https URL under " + PROFILE_PIC_MAX_LENGTH + " characters"));
        }

        User existing = existingOptional.get();

        if(dto.profilePic() != null) existing.setProfilePic(dto.profilePic());
        if(dto.bio() != null) existing.setBio(dto.bio());

        repo.save(existing);
        return ResponseEntity.ok(existing);
    }

    private boolean isValidProfilePicUrl(String value) {
        if (value.length() > PROFILE_PIC_MAX_LENGTH) return false;
        try {
            URI uri = new URI(value);
            return "https".equalsIgnoreCase(uri.getScheme());
        } catch (URISyntaxException e) {
            return false;
        }
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
