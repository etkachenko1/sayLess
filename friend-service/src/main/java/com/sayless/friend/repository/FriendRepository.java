package com.sayless.friend.repository;

import com.sayless.friend.model.Friends;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends MongoRepository<Friends, String> {
    List<Friends> findByRequesterIdOrReceiverId(String requesterId, String receiverId);
    Optional<Friends> findByRequesterIdAndReceiverId(String requesterId, String receiverId);
    
}
