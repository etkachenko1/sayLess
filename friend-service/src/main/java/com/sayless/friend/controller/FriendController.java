package com.sayless.friend.controller;

import com.sayless.friend.model.Friends;
import com.sayless.friend.client.UserClient;
import com.sayless.friend.repository.FriendRepository;
import com.sayless.friend.dto.FriendDto;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/friends")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class FriendController {
    private final FriendRepository repo;
    private final UserClient userClient;
    private final RestTemplate restTemplate = new RestTemplate();



    public FriendController(FriendRepository repo, UserClient userClient) {
        this.repo = repo;
        this.userClient = userClient;

    }

    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    @PostMapping("/request/")
    public ResponseEntity<?> sendRequest(@RequestParam String receiverId, Authentication auth) {
        String me = uid(auth);
        if (me.equals(receiverId)) return ResponseEntity.badRequest().body("Cannot friend yourself!");

        Optional<Friends> existing = repo.findByRequesterIdAndReceiverId(me, receiverId);
        if (existing.isPresent()) return ResponseEntity.badRequest().body("Request already exists");

        Friends f = new Friends();
        f.setRequesterId(me);
        f.setReceiverId(receiverId);
        f.setStatus(Friends.Status.PENDING);
        return ResponseEntity.ok(repo.save(f));
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptRequest(@RequestParam String requesterId, Authentication auth) {
        String me = uid(auth);
        Optional<Friends> opt = repo.findByRequesterIdAndReceiverId(requesterId, me);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Friends f = opt.get();
        f.setStatus(Friends.Status.ACCEPTED);
        return ResponseEntity.ok(repo.save(f));
    }

    @GetMapping
    public List<FriendDto> getAll(Authentication auth) {
        String me = uid(auth);
        List<Friends> all = repo.findByRequesterIdOrReceiverId(me, me);
    
        return all.stream().map((Friends f) -> new FriendDto(
            f.getId(),
            f.getRequesterId(),
            userClient.getUsername(f.getRequesterId()),
            f.getReceiverId(),
            userClient.getUsername(f.getReceiverId()),
            f.getStatus().name(),
            f.getCreatedAt()
        )).toList();
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFriend(@RequestParam String friendId, Authentication auth) {
        String me = uid(auth);
        var all = repo.findByRequesterIdOrReceiverId(me, me);
        all.stream()
           .filter(f -> (f.getRequesterId().equals(friendId) || f.getReceiverId().equals(friendId)))
           .findFirst()
           .ifPresent(repo::delete);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String username) {
    try {
        // call auth service
        String url = "http://localhost:8081/users/search?username=" + username;
        Object[] users = restTemplate.getForObject(url, Object[].class);
        return ResponseEntity.ok(users);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Search failed");
    }
}
}
