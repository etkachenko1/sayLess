package com.sayless.task.dto;
import java.time.Instant;
public record TaskUpdateDto(String title, String description, Instant deadline, String assignedTo) {}

