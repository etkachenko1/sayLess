package com.sayless.notification.controller;

import com.sayless.notification.model.Notification;
import com.sayless.notification.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    @GetMapping
    public List<Notification> getAll(Authentication auth) {
        return repo.findByUserIdOrderByCreatedAtDesc(uid(auth));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id, Authentication auth) {
        Optional<Notification> opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Notification n = opt.get();
        if (!n.getUserId().equals(uid(auth))) return ResponseEntity.status(403).build();

        n.setRead(true);
        return ResponseEntity.ok(repo.save(n));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication auth) {
        Optional<Notification> opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Notification n = opt.get();
        if (!n.getUserId().equals(uid(auth))) return ResponseEntity.status(403).build();

        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}