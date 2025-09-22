//DB Access
package com.sayless.auth.repository;

import com.sayless.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository; //for find, save, delete mongodb oper-s
import java.util.Optional; //to prevent null bugs

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
} 