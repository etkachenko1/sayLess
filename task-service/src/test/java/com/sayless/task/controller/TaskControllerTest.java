package com.sayless.task.controller;

import com.sayless.task.client.UserClient;
import com.sayless.task.dto.StatusUpdateDto;
import com.sayless.task.dto.TaskCreateDto;
import com.sayless.task.dto.TaskResponseDto;
import com.sayless.task.dto.TaskUpdateDto;
import com.sayless.task.event.TaskAssignedEvent;
import com.sayless.task.event.TaskCompletedEvent;
import com.sayless.task.event.TaskCreatedEvent;
import com.sayless.task.event.TaskDeletedEvent;
import com.sayless.task.event.TaskUpdatedEvent;
import com.sayless.task.kafka.TaskEventProducer;
import com.sayless.task.model.Task;
import com.sayless.task.repo.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit coverage for TaskController's CRUD logic and Kafka event selection, calling the controller
// directly with mocked collaborators. Ownership checks are also covered end-to-end through the
// JWT filter in TaskControllerSecurityTest.
class TaskControllerTest {

    private TaskRepository repo;
    private UserClient userClient;
    private TaskEventProducer eventProducer;
    private TaskController controller;

    @BeforeEach
    void setUp() {
        repo = mock(TaskRepository.class);
        userClient = mock(UserClient.class);
        eventProducer = mock(TaskEventProducer.class);
        controller = new TaskController(repo, userClient, eventProducer);

        when(repo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getUsername(any())).thenAnswer(inv -> inv.getArgument(0) + "-name");
    }

    private Authentication authFor(String userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Task task(String id, String createdBy, String assignedTo) {
        return new Task(id, "title", "desc", Task.Status.TODO, assignedTo, createdBy,
                Instant.parse("2030-01-01T00:00:00Z"), Instant.now(), Instant.now());
    }

    @Test
    void getAllTasks_queriesOnlyTheCallersTasks_andResolvesUsernames() {
        when(repo.findByCreatedByOrAssignedTo("alice-id", "alice-id"))
                .thenReturn(List.of(task("t1", "alice-id", "bob-id")));

        List<TaskResponseDto> result = controller.getAllTasks(authFor("alice-id"));

        assertEquals(1, result.size());
        assertEquals("t1", result.get(0).id());
        assertEquals("alice-id-name", result.get(0).createdByName());
        assertEquals("bob-id-name", result.get(0).assignedToName());
        verify(repo).findByCreatedByOrAssignedTo("alice-id", "alice-id");
    }

    @Test
    void getTasksAssignedToMe_andAssignedByMe_useTheCallersId() {
        controller.getTasksAssignedToMe(authFor("alice-id"));
        controller.getTasksAssignedByMe(authFor("alice-id"));

        verify(repo).findByAssignedTo("alice-id");
        verify(repo).findByCreatedBy("alice-id");
    }

    @Test
    void createTask_setsCreatorFromAuth_defaultsAssigneeToCreator_andPublishesCreated() {
        ResponseEntity<?> res = controller.createTask(
                new TaskCreateDto("Write tests", "desc", null, null), authFor("alice-id"));

        assertEquals(200, res.getStatusCode().value());
        Task saved = (Task) res.getBody();
        assertEquals("alice-id", saved.getCreatedBy());
        assertEquals("alice-id", saved.getAssignedTo());
        assertEquals(Task.Status.TODO, saved.getStatus());
        assertNotNull(saved.getCreatedAt());

        ArgumentCaptor<TaskCreatedEvent> event = ArgumentCaptor.forClass(TaskCreatedEvent.class);
        verify(eventProducer).publish(event.capture());
        assertEquals("Write tests", event.getValue().getTitle());
    }

    @Test
    void createTask_withAssignee_assignsToThatUser() {
        ResponseEntity<?> res = controller.createTask(
                new TaskCreateDto("Write tests", null, null, "bob-id"), authFor("alice-id"));

        Task saved = (Task) res.getBody();
        assertEquals("alice-id", saved.getCreatedBy());
        assertEquals("bob-id", saved.getAssignedTo());
    }

    @Test
    void createTask_blankTitle_isRejected() {
        ResponseEntity<?> res = controller.createTask(
                new TaskCreateDto("  ", null, null, null), authFor("alice-id"));

        assertEquals(400, res.getStatusCode().value());
        verify(repo, never()).save(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void createTask_titleOverMaxLength_isRejected() {
        ResponseEntity<?> res = controller.createTask(
                new TaskCreateDto("x".repeat(151), null, null, null), authFor("alice-id"));

        assertEquals(400, res.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void updateTask_byCreator_appliesNonNullFields_andPublishesUpdated() {
        Task existing = task("t1", "alice-id", "alice-id");
        when(repo.findById("t1")).thenReturn(Optional.of(existing));

        ResponseEntity<?> res = controller.updateTask("t1",
                new TaskUpdateDto("New title", null, null, null), authFor("alice-id"));

        assertEquals(200, res.getStatusCode().value());
        Task saved = (Task) res.getBody();
        assertEquals("New title", saved.getTitle());
        assertEquals("desc", saved.getDescription());
        verify(eventProducer).publishUpdated(any(TaskUpdatedEvent.class));
        verify(eventProducer, never()).publishAssigned(any());
    }

    @Test
    void updateTask_byCreator_withNewAssignee_publishesAssignedWithPreviousAssignee() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "bob-id")));

        controller.updateTask("t1", new TaskUpdateDto(null, null, null, "carol-id"), authFor("alice-id"));

        ArgumentCaptor<TaskAssignedEvent> event = ArgumentCaptor.forClass(TaskAssignedEvent.class);
        verify(eventProducer).publishAssigned(event.capture());
        assertEquals("carol-id", event.getValue().getAssignedToId());
        assertEquals("bob-id", event.getValue().getPreviousAssigneeId());
        verify(eventProducer, never()).publishUpdated(any());
    }

    @Test
    void updateTask_descriptionOverMaxLength_isRejected() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "alice-id")));

        ResponseEntity<?> res = controller.updateTask("t1",
                new TaskUpdateDto(null, "x".repeat(1001), null, null), authFor("alice-id"));

        assertEquals(400, res.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    @Test
    void updateTask_missingTask_is404() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        ResponseEntity<?> res = controller.updateTask("missing",
                new TaskUpdateDto("x", null, null, null), authFor("alice-id"));

        assertEquals(404, res.getStatusCode().value());
    }

    @Test
    void assignTask_byCreator_reassigns_andPublishesAssigned() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "alice-id")));

        ResponseEntity<?> res = controller.assignTask("t1", "bob-id", authFor("alice-id"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals("bob-id", ((Task) res.getBody()).getAssignedTo());
        ArgumentCaptor<TaskAssignedEvent> event = ArgumentCaptor.forClass(TaskAssignedEvent.class);
        verify(eventProducer).publishAssigned(event.capture());
        assertEquals("alice-id", event.getValue().getPreviousAssigneeId());
    }

    @Test
    void updateStatus_toDone_publishesCompletedWithTheCaller() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "bob-id")));

        ResponseEntity<?> res = controller.updateStatus("t1", new StatusUpdateDto("DONE"), authFor("bob-id"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals(Task.Status.DONE, ((Task) res.getBody()).getStatus());
        ArgumentCaptor<TaskCompletedEvent> event = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventProducer).publishCompleted(event.capture());
        assertEquals("bob-id", event.getValue().getCompletedById());
    }

    @Test
    void updateStatus_toInProgress_publishesUpdated() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "bob-id")));

        controller.updateStatus("t1", new StatusUpdateDto("IN_PROGRESS"), authFor("alice-id"));

        verify(eventProducer).publishUpdated(any(TaskUpdatedEvent.class));
        verify(eventProducer, never()).publishCompleted(any());
    }

    @Test
    void updateStatus_invalidValue_isRejected() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "alice-id")));

        ResponseEntity<?> res = controller.updateStatus("t1", new StatusUpdateDto("ARCHIVED"), authFor("alice-id"));

        assertEquals(400, res.getStatusCode().value());
        verify(repo, never()).save(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void deleteTask_byCreator_deletes_andPublishesDeleted() {
        when(repo.findById("t1")).thenReturn(Optional.of(task("t1", "alice-id", "bob-id")));

        ResponseEntity<?> res = controller.deleteTask("t1", authFor("alice-id"));

        assertEquals(204, res.getStatusCode().value());
        verify(repo).deleteById("t1");
        ArgumentCaptor<TaskDeletedEvent> event = ArgumentCaptor.forClass(TaskDeletedEvent.class);
        verify(eventProducer).publishDeleted(event.capture());
        assertEquals("t1", event.getValue().getTaskId());
    }

    @Test
    void deleteTask_missingTask_is404() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        ResponseEntity<?> res = controller.deleteTask("missing", authFor("alice-id"));

        assertEquals(404, res.getStatusCode().value());
        verify(repo, never()).deleteById(any());
    }
}
