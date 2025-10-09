package com.sayless.friend.controller;

import com.sayless.friend.model.Friends;
import com.sayless.friend.repository.FriendRepository;
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

    public FriendController(FriendRepository repo) {
        this.repo = repo;
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
    public List<Friends> getAll(Authentication auth) {
        String me = uid(auth);
        return repo.findByRequesterIdOrReceiverId(me, me);
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
}
