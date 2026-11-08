package com.sayless.task.controller;

import com.sayless.task.client.UserClient;
import com.sayless.task.kafka.TaskEventProducer;
import com.sayless.task.model.Task;
import com.sayless.task.repo.TaskRepository;
import com.sayless.task.security.JwtAuthFilter;
import com.sayless.task.security.JwtVerifier;
import com.sayless.task.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration coverage for TaskController's ownership checks, through the real security filter
// chain and JWT verification with the repository, Kafka producer, and auth-service client mocked.
// Only a task's creator can edit, reassign, or delete it; its creator or assignee can change its
// status; and listing endpoints only ever query the caller's own tasks. A user with no relation to
// a task can't touch it, and a rejected request must never save, delete, or publish an event.
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtVerifier.class})
@TestPropertySource(properties = {
    "jwt.secret=test-only-secret-not-used-anywhere-else-0123456789",
    "cors.allowed-origins=http://localhost:5173"
})
class TaskControllerSecurityTest {

    private static final String SECRET = "test-only-secret-not-used-anywhere-else-0123456789";
    private static final String TASK_ID = "task-1";
    private static final String CREATOR = "creator-id";
    private static final String ASSIGNEE = "assignee-id";
    private static final String STRANGER = "stranger-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private TaskEventProducer eventProducer;

    @MockitoBean
    private UserClient userClient;

    private String tokenFor(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    private Task existingTask() {
        return new Task(TASK_ID, "title", "desc", Task.Status.TODO, ASSIGNEE, CREATOR,
                Instant.parse("2030-01-01T00:00:00Z"), Instant.now(), Instant.now());
    }

    @BeforeEach
    void setUp() {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existingTask()));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getUsername(anyString())).thenReturn("someone");
    }

    private void assertNothingWasWritten() {
        verify(taskRepository, never()).save(any());
        verify(taskRepository, never()).deleteById(any());
        verifyNoInteractions(eventProducer);
    }

    // --- authentication ---

    @Test
    void getTasks_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(taskRepository);
    }

    @Test
    void getTasks_withTokenSignedByAnotherKey_isRejected() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor("some-other-secret-that-is-long-enough-0123456789".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().setSubject(CREATOR)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey).compact();

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + forged))
                .andExpect(status().isForbidden());

        verifyNoInteractions(taskRepository);
    }

    @Test
    void createTask_withoutToken_isRejected() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType("application/json")
                        .content("{\"title\":\"t\"}"))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void deleteTask_withoutToken_isRejected() throws Exception {
        mockMvc.perform(delete("/tasks/" + TASK_ID))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    // --- read ---

    @Test
    void getTasks_onlyQueriesTheCallersOwnTasks() throws Exception {
        when(taskRepository.findByCreatedByOrAssignedTo(STRANGER, STRANGER)).thenReturn(List.of());

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + tokenFor(STRANGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(taskRepository).findByCreatedByOrAssignedTo(STRANGER, STRANGER);
        verify(taskRepository, never()).findByCreatedByOrAssignedTo(argThat(id -> !STRANGER.equals(id)), any());
        verify(taskRepository, never()).findAll();
    }

    @Test
    void getTasks_returnsTheCallersTasks() throws Exception {
        when(taskRepository.findByCreatedByOrAssignedTo(CREATOR, CREATOR)).thenReturn(List.of(existingTask()));

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + tokenFor(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TASK_ID))
                .andExpect(jsonPath("$[0].createdById").value(CREATOR))
                .andExpect(jsonPath("$[0].assignedToId").value(ASSIGNEE));
    }

    @Test
    void assignedToMe_andAssignedByMe_onlyQueryTheCaller() throws Exception {
        mockMvc.perform(get("/tasks/assigned-to-me").header("Authorization", "Bearer " + tokenFor(STRANGER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/tasks/assigned-by-me").header("Authorization", "Bearer " + tokenFor(STRANGER)))
                .andExpect(status().isOk());

        verify(taskRepository).findByAssignedTo(STRANGER);
        verify(taskRepository).findByCreatedBy(STRANGER);
        verify(taskRepository, never()).findByAssignedTo(argThat(id -> !STRANGER.equals(id)));
        verify(taskRepository, never()).findByCreatedBy(argThat(id -> !STRANGER.equals(id)));
    }

    // --- create ---

    @Test
    void createTask_ignoresAnyCreatorInTheBody_andUsesTheTokenSubject() throws Exception {
        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenFor(STRANGER))
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"createdBy\":\"" + CREATOR + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value(STRANGER))
                .andExpect(jsonPath("$.assignedTo").value(STRANGER));

        verify(eventProducer).publish(any());
    }

    // --- update ---

    @Test
    void updateTask_byStranger_isForbidden() throws Exception {
        mockMvc.perform(put("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(STRANGER))
                        .contentType("application/json")
                        .content("{\"title\":\"hijacked\"}"))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void updateTask_byAssignee_isForbidden() throws Exception {
        mockMvc.perform(put("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(ASSIGNEE))
                        .contentType("application/json")
                        .content("{\"title\":\"edited\"}"))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void updateTask_byCreator_isAllowed() throws Exception {
        mockMvc.perform(put("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(CREATOR))
                        .contentType("application/json")
                        .content("{\"title\":\"edited\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("edited"));

        verify(taskRepository).save(any());
        verify(eventProducer).publishUpdated(any());
    }

    @Test
    void updateTask_onMissingTask_is404() throws Exception {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(put("/tasks/missing")
                        .header("Authorization", "Bearer " + tokenFor(CREATOR))
                        .contentType("application/json")
                        .content("{\"title\":\"edited\"}"))
                .andExpect(status().isNotFound());

        assertNothingWasWritten();
    }

    // --- reassign ---

    @Test
    void reassignViaUpdate_byStranger_isForbidden() throws Exception {
        mockMvc.perform(put("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(STRANGER))
                        .contentType("application/json")
                        .content("{\"assignedTo\":\"" + STRANGER + "\"}"))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void assignEndpoint_byStranger_isForbidden() throws Exception {
        mockMvc.perform(post("/tasks/assign")
                        .header("Authorization", "Bearer " + tokenFor(STRANGER))
                        .param("taskId", TASK_ID)
                        .param("userId", STRANGER))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void assignEndpoint_byAssignee_isForbidden() throws Exception {
        mockMvc.perform(post("/tasks/assign")
                        .header("Authorization", "Bearer " + tokenFor(ASSIGNEE))
                        .param("taskId", TASK_ID)
                        .param("userId", STRANGER))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void assignEndpoint_byCreator_isAllowed() throws Exception {
        mockMvc.perform(post("/tasks/assign")
                        .header("Authorization", "Bearer " + tokenFor(CREATOR))
                        .param("taskId", TASK_ID)
                        .param("userId", "new-assignee-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value("new-assignee-id"));

        verify(eventProducer).publishAssigned(any());
    }

    // --- status ---

    @Test
    void updateStatus_byStranger_isForbidden() throws Exception {
        mockMvc.perform(patch("/tasks/" + TASK_ID + "/status")
                        .header("Authorization", "Bearer " + tokenFor(STRANGER))
                        .contentType("application/json")
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void updateStatus_byAssignee_isAllowed() throws Exception {
        mockMvc.perform(patch("/tasks/" + TASK_ID + "/status")
                        .header("Authorization", "Bearer " + tokenFor(ASSIGNEE))
                        .contentType("application/json")
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        verify(eventProducer).publishCompleted(any());
    }

    @Test
    void updateStatus_byCreator_isAllowed() throws Exception {
        mockMvc.perform(patch("/tasks/" + TASK_ID + "/status")
                        .header("Authorization", "Bearer " + tokenFor(CREATOR))
                        .contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(eventProducer).publishUpdated(any());
    }

    // --- delete ---

    @Test
    void deleteTask_byStranger_isForbidden() throws Exception {
        mockMvc.perform(delete("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(STRANGER)))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void deleteTask_byAssignee_isForbidden() throws Exception {
        mockMvc.perform(delete("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(ASSIGNEE)))
                .andExpect(status().isForbidden());

        assertNothingWasWritten();
    }

    @Test
    void deleteTask_byCreator_isAllowed() throws Exception {
        mockMvc.perform(delete("/tasks/" + TASK_ID)
                        .header("Authorization", "Bearer " + tokenFor(CREATOR)))
                .andExpect(status().isNoContent());

        verify(taskRepository).deleteById(TASK_ID);
        verify(eventProducer).publishDeleted(any());
    }
}
