package com.sayless.friend.controller;

import com.sayless.friend.model.Friends;
import com.sayless.friend.client.UserClient;
import com.sayless.friend.repository.FriendRepository;
import com.sayless.friend.dto.FriendDto;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.*;

/** I love your work but I have left comments for best practices which obviously I dont even expect from good engineers but thats what
 * makes you amazing. So I am glad that I can point you there. YOu dont need to  understand everything and you can ignore if it doesnt make sense but even familairity would make you amazing for any interview
 *
 */


/**
 * Great work but if you wish to enhance your skill a little and be a great engineer read about two different kind of Design Patters 1.) DAO based 2.) Service oriented
 * you have used service repo oriented pattern. YFInd out when is DAO used over service
 * Its all about best practices and design patterns
 * **/
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

    @PostMapping("/request")
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


    /**
     * So read about streaming and optionals and look into below implementation
     * @DeleteMapping("/remove")
     * public ResponseEntity<?> removeFriend(@RequestParam String friendId, Authentication auth) {
     *     String me = uid(auth);
     *     repo.deleteByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(me, friendId, friendId, me);
     *     return ResponseEntity.noContent().build();
     * }
     *
     * For cases where you are quring by a unique identifier such as id in this case and you are expecting one result .
     * YOu use the abive way cuzz its optimized . Stream collects and then runs filter in it and is better used when you are expecting more than one. So calling .first int he result  says it all that you dont need to use it here
     * Also read about 'Optionals' in Java
     */
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
    public ResponseEntity<?> searchUsers(@RequestParam String username, Authentication auth) {
        String me = uid(auth);


        /**
         *  this is solid work, but just for learning sake
         * im gonna walk through two best practices that separate
         *  someone who is great from someone who is truly badass lol
         *  PLease do some research on these cuz i just gave you pointers if you want to learn more
         1. Custom Exceptions
         Always create custom exception class that extends Runtime excption.
         Each excepton should represent a specific failure case for example UserNotFoundException/InvalidReqestException.
         The busniess logic should be in a seperate service class not the controller.
         When error happens throw the custom exception from there. it will bubble up to controller where you can catch it and just return a generic reponse like 503.
         This keeps code clean and modular, makes logs easy to read and hides internal details from users, they only see a genric error.

         2. Api Client Classes
         When calling another api make a client class, like UserServiceClient.
         That class should handle request creation, headers, auth and reading url from application.properties file.
         Keeping all that logic in one place helps you reuse code and avoid stupid mistakes.
         You can easily switch between dev staging and prod by changing properties, no need to touch code again.

         3. Avoid hardcoded strings
         Dont hardcode urls keys or random text in code, looks messy and breaks easy.
         Use constants for fixed values, enums for grouped stuff and property files for configs that can change per env.
         You can inject them using @Value or @ConfigurationProperties.
         Property files live inside resource folder you can have like application-dev.properties or application-prod.properties etc.
         which jhelps swith between different environmemnts with just swithcing the source .

         4. Dependency Injection
         Learn how Spring boot IOC container works, its kinda the core thing.
         DI helps to write cleaner and testable code.
         Instead of new SomeService() you let Spring handle it using @Autowired or constructor injection.
         Understanding DI is what really separates an engineer from just a developer.

         You should be also able to answer whats JPA , Hbernate , Spring -data and what are differences . Which one is abstraction and which one is really the implemntation among those
         IMportant

         5. Design patterns and Beans
         Read how beans work in Springboot what they do and how scopes like singleton or prototype behave.
         Also read about design paterns like singleton factory strategy etx they help in writing modular and reusable stuf
         */

        try {
        // call auth servic
            /** YOu have a user client defined in the project . Why are you not using it ? Thats exactly I was talking about in my second point above of best practices**/
        String url = "http://localhost:8081/users/search?username=" + username;
        Object[] users = restTemplate.getForObject(url, Object[].class);
        List<Object> filtered = Arrays.stream(users).filter(u-> {
            if(u instanceof Map<?,?> map) {
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
