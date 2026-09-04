package com.sayless.task.controller;
import com.sayless.task.event.TaskCreatedEvent;
import com.sayless.task.event.TaskAssignedEvent;
import com.sayless.task.event.TaskCompletedEvent;
import com.sayless.task.event.TaskDeletedEvent;
import com.sayless.task.event.TaskUpdatedEvent;
import com.sayless.task.kafka.TaskEventProducer;

import com.sayless.task.client.UserClient;
import com.sayless.task.dto.TaskCreateDto;
import com.sayless.task.dto.TaskResponseDto;
import com.sayless.task.dto.TaskUpdateDto;
import com.sayless.task.dto.StatusUpdateDto;
import com.sayless.task.model.Task;
import com.sayless.task.repo.TaskRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; //annotations to declare REST endpoints
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@RestController //marks this as Rest api controller, so Spring will automatically expose methods as endpoints
@RequestMapping("/tasks") //all endpoints here will begin with /tasks
public class TaskController {
    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    //MongoDb repo for Task collection
    private final TaskRepository repo;
    private final UserClient userClient;
    private final TaskEventProducer eventProducer;

    public TaskController(TaskRepository repo, UserClient userClient, TaskEventProducer eventProducer) {
        this.repo = repo;
        this.userClient = userClient;
        this.eventProducer = eventProducer;
    }

    //helper to exctract userId stored as principal in jwtAuthFilter from token
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    private String validateTitleAndDescription(String title, String description, boolean titleRequired) {
        if (titleRequired && (title == null || title.isBlank())) {
            return "Title is required";
        }
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            return "Title must be " + MAX_TITLE_LENGTH + " characters or fewer";
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            return "Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer";
        }
        return null;
    }

    //GET //tasks -> all tasks createdby or assignedTo user
    @GetMapping
    public List<TaskResponseDto> getAllTasks(Authentication auth) {
        String me = uid(auth);
        var tasks= repo.findByCreatedByOrAssignedTo(me, me);
        return tasks.stream().map(t -> {
            String createdByName = userClient.getUsername(t.getCreatedBy());
            String assignedToName = userClient.getUsername(t.getAssignedTo());
            return new TaskResponseDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus().name(),
                t.getDeadline(),
                t.getCreatedBy(),
                createdByName,
                t.getAssignedTo(),
                assignedToName,
                t.getUpdatedAt()
            );
        }).toList();
        }

    @GetMapping("/assigned-to-me")
    public List<Task> getTasksAssignedToMe(Authentication auth){
        String me = uid(auth);
        return repo.findByAssignedTo(me);
    }

    @GetMapping("/assigned-by-me")
    public List<Task> getTasksAssignedByMe(Authentication auth){
        String me = uid(auth);
        return repo.findByCreatedBy(me);
    }

    //POST /tasks
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskCreateDto dto, Authentication auth) {
        String validationError = validateTitleAndDescription(dto.title(), dto.description(), true);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        String me = uid(auth);
        Task t = new Task();
        t.setTitle(dto.title());
        t.setDescription(dto.description());
        t.setStatus(Task.Status.TODO);
        t.setDeadline(dto.deadline());
        t.setAssignedTo(dto.assignedTo() == null ? me : dto.assignedTo());
        t.setCreatedBy(me);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        Task saved = repo.save(t);
        eventProducer.publish(
            new TaskCreatedEvent(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getDeadline(),
                saved.getStatus().name(),
                saved.getAssignedTo(),
                userClient.getUsername(saved.getAssignedTo()),
                saved.getCreatedBy(),
                userClient.getUsername(saved.getCreatedBy()),
                saved.getUpdatedAt()
            )
        );
        return ResponseEntity.ok(saved);
    }

    //post /tasks/assign

    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(@RequestParam String taskId, @RequestParam String userId, Authentication auth) {
        String me = uid(auth);
        Optional<Task> opt = repo.findById(taskId);
        if(opt.isEmpty()) return ResponseEntity.notFound().build();

        Task t = opt.get();
        if(!t.getCreatedBy().equals(me)) {
            return ResponseEntity.status(403).body("Only the creator can assign this task");
        }
        String previousAssignedTo = t.getAssignedTo();
        t.setAssignedTo(userId);
        t.setUpdatedAt(Instant.now());
        Task saved = repo.save(t);
        eventProducer.publishAssigned(
            new TaskAssignedEvent(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getDeadline(),
                saved.getStatus().name(),
                saved.getAssignedTo(),
                userClient.getUsername(saved.getAssignedTo()),
                saved.getCreatedBy(),
                userClient.getUsername(saved.getCreatedBy()),
                previousAssignedTo,
                saved.getUpdatedAt()
            )
        );
        return ResponseEntity.ok(saved); }

        //update task
        @PutMapping("/{id}")
        public ResponseEntity<?> updateTask(@PathVariable String id, @RequestBody TaskUpdateDto dto, Authentication auth){
            String me =  uid(auth);
            Optional <Task> opt = repo.findById(id);
            if(opt.isEmpty()) return ResponseEntity.notFound().build();

            Task t = opt.get();
            if(!t.getCreatedBy().equals(me)) {
                return ResponseEntity.status(403).build();

            }
            String validationError = validateTitleAndDescription(dto.title(), dto.description(), false);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", validationError));
            }
            String previousAssignedTo = t.getAssignedTo();
            if(dto.title()!= null)  t.setTitle(dto.title());
            if(dto.description()!= null)  t.setDescription(dto.description());
            if(dto.deadline()!= null)  t.setDeadline(dto.deadline());
            if(dto.assignedTo()!= null)  t.setAssignedTo(dto.assignedTo());
            t.setUpdatedAt(Instant.now());

            Task saved = repo.save(t);
            boolean reassigned = dto.assignedTo() != null && !dto.assignedTo().equals(previousAssignedTo);
            if (reassigned) {
                eventProducer.publishAssigned(
                    new TaskAssignedEvent(
                        saved.getId(),
                        saved.getTitle(),
                        saved.getDescription(),
                        saved.getDeadline(),
                        saved.getStatus().name(),
                        saved.getAssignedTo(),
                        userClient.getUsername(saved.getAssignedTo()),
                        saved.getCreatedBy(),
                        userClient.getUsername(saved.getCreatedBy()),
                        previousAssignedTo,
                        saved.getUpdatedAt()
                    )
                );
            } else {
                eventProducer.publishUpdated(
                    new TaskUpdatedEvent(
                        saved.getId(),
                        saved.getTitle(),
                        saved.getDescription(),
                        saved.getDeadline(),
                        saved.getStatus().name(),
                        saved.getAssignedTo(),
                        userClient.getUsername(saved.getAssignedTo()),
                        saved.getCreatedBy(),
                        userClient.getUsername(saved.getCreatedBy()),
                        me,
                        userClient.getUsername(me),
                        saved.getUpdatedAt()
                    )
                );
            }
            return ResponseEntity.ok(saved); }

        //task status update
        @PatchMapping("/{id}/status")
        public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody StatusUpdateDto dto, Authentication auth) {
            String me = uid(auth);
            Optional <Task> opt = repo.findById(id);
            if(opt.isEmpty()) return ResponseEntity.notFound().build();

            Task t = opt.get();
            if(!t.getCreatedBy().equals(me) && !t.getAssignedTo().equals(me)) {
                return ResponseEntity.status(403).build();}

            Task.Status newStatus;
            try {
                newStatus = Task.Status.valueOf(dto.status());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + dto.status()));
            }
            t.setStatus(newStatus);
            t.setUpdatedAt(Instant.now());
            Task saved = repo.save(t);
            if (newStatus == Task.Status.DONE) {
                eventProducer.publishCompleted(
                    new TaskCompletedEvent(
                        saved.getId(),
                        saved.getTitle(),
                        saved.getDescription(),
                        saved.getDeadline(),
                        saved.getStatus().name(),
                        saved.getAssignedTo(),
                        userClient.getUsername(saved.getAssignedTo()),
                        saved.getCreatedBy(),
                        userClient.getUsername(saved.getCreatedBy()),
                        me,
                        userClient.getUsername(me),
                        saved.getUpdatedAt()
                    )
                );
            } else {
                eventProducer.publishUpdated(
                    new TaskUpdatedEvent(
                        saved.getId(),
                        saved.getTitle(),
                        saved.getDescription(),
                        saved.getDeadline(),
                        saved.getStatus().name(),
                        saved.getAssignedTo(),
                        userClient.getUsername(saved.getAssignedTo()),
                        saved.getCreatedBy(),
                        userClient.getUsername(saved.getCreatedBy()),
                        me,
                        userClient.getUsername(me),
                        saved.getUpdatedAt()
                    )
                );
            }
            return ResponseEntity.ok(saved);

        }

        //DELETE

        @DeleteMapping("/{id}")
        public ResponseEntity<?> deleteTask(@PathVariable String id, Authentication auth){
            String me = uid(auth);
            Optional<Task> opt = repo.findById(id);
            if(opt.isEmpty()) return ResponseEntity.notFound().build();

            Task t = opt.get();
            if (!t.getCreatedBy().equals(me)) {return ResponseEntity.status(403).build();}

            repo.deleteById(id);
            eventProducer.publishDeleted(
                new TaskDeletedEvent(t.getId(), t.getCreatedBy(), t.getAssignedTo())
            );
            return ResponseEntity.noContent().build();
        }

        
}
