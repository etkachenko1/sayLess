package com.sayless.task.controller;

import com.sayless.task.dto.TaskCreateDto;
import com.sayless.task.dto.TaskUpdateDto;
import com.sayless.task.dto.StatusUpdateDto;
import com.sayless.task.model.Task;
import com.sayless.task.repo.TaskRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; //annotations to declare REST endpoints
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@RestController //marks this as Rest api controller, so Spring will automatically expose methods as endpoints
@RequestMapping("/tasks") //all endpoints here will begin with /tasks
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})


public class TaskController {
    //MongoDb repo for Task collection
    private final TaskRepository repo;
 
    public TaskController(TaskRepository repo) {
        this.repo = repo;
    }

    //helper to exctract userId stored as principal in jwtAuthFilter from token
    private String uid(Authentication auth) {
        return (String) auth.getPrincipal();
    }

    //GET //tasks -> all tasks createdby or assignedTo user
    @GetMapping
    public List<Task> getAllTasks(Authentication auth) {
        String me = uid(auth);
        return repo.findByCreatedByOrAssignedTo(me, me);
    }

    @GetMapping("/assigned-to-me")
    public List<Task> getTasksAssignedToMe(Authentication auth){
        String me = uid(auth);
        return repo.findByAssignedTo(me);
    }

    @GetMapping("/assitgned-by-me")
    public List<Task> getTasksAssignedByMe(Authentication auth){
        String me = uid(auth);
        return repo.findByCreatedBy(me);
    }

    //POST /tasks
    @PostMapping
    public Task createTask(@RequestBody TaskCreateDto dto, Authentication auth) {
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
        return repo.save(t);
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
        t.setAssignedTo(userId);
        t.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(repo.save(t)); }

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
            if(dto.title()!= null)  t.setTitle(dto.title());
            if(dto.description()!= null)  t.setDescription(dto.description());
            if(dto.deadline()!= null)  t.setDeadline(dto.deadline());
            if(dto.assignedTo()!= null)  t.setAssignedTo(dto.assignedTo());
            t.setUpdatedAt(Instant.now());

            return ResponseEntity.ok(repo.save(t));


        }

        //task status update
        @PatchMapping("/{id}/status")
        public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody StatusUpdateDto dto, Authentication auth) {
            String me = uid(auth);
            Optional <Task> opt = repo.findById(id);
            if(opt.isEmpty()) return ResponseEntity.notFound().build();

            Task t = opt.get();
            if(!t.getCreatedBy().equals(me) && !t.getAssignedTo().equals(me)) {
                return ResponseEntity.status(403).build();}

            Task.Status newStatus =  Task.Status.valueOf(dto.status());
            t.setStatus(newStatus);
            t.setUpdatedAt(Instant.now());
            return ResponseEntity.ok(repo.save(t));

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
            return ResponseEntity.noContent().build();
        }

        
}
