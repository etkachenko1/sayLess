package com.sayless.friend.controller;

import com.sayless.friend.model.Friends;
import com.sayless.friend.client.UserClient;
import com.sayless.friend.repository.FriendRepository;
import com.sayless.friend.dto.FriendDto;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/friends")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class FriendController {
    private final FriendRepository repo;
    private final UserClient userClient;

    public FriendController(FriendRepository repo, UserClient userClient) {
        this.repo = repo;
        this.userClient = userClient;
    }

    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestParam String receiverId, Authentication auth) {
        String me = uid(auth);
        if (me.equals(receiverId)) return ResponseEntity.badRequest().body("Cannot friend yourself!");

        Optional<Friends> existing = repo.findFirstByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(me, receiverId, receiverId, me);
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
        repo.deleteByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(me, friendId, friendId, me);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String username, Authentication auth) {
        String me = uid(auth);
        try {
            Object[] users = userClient.searchUsers(username);
            List<Object> filtered = Arrays.stream(users).filter(u -> {
                if (u instanceof Map<?, ?> map) {
                    Object id = map.get("id");
                    return id != null && !id.equals(me);
                }
                return true;
            }).toList();
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Search failed");
        }
    }

    @GetMapping("/accepted")
    public ResponseEntity<?> getAcceptedFriends(Authentication auth) {
        String me = uid(auth);
        List<Friends> accepted = repo.findByRequesterIdOrReceiverId(me, me).stream()
            .filter(f -> f.getStatus() == Friends.Status.ACCEPTED)
            .toList();

        var friendDtos = accepted.stream().map(f -> {
            boolean amRequester = f.getRequesterId().equals(me);
            String friendId = amRequester ? f.getReceiverId() : f.getRequesterId();
            String friendName = userClient.getUsername(friendId);
            return Map.of(
                "id", friendId,
                "username", friendName
            );
        }).toList();

        return ResponseEntity.ok(friendDtos);
    }
}
