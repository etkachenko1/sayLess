package com.sayless.task.repo;
import com.sayless.task.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task,String> {
    List<Task> findByCreatedBy(String createdBy);
    List<Task> findByAssignedTo(String assignedTo);
    List<Task> findByCreatedByOrAssignedTo(String createdBy, String assignedTo);

}