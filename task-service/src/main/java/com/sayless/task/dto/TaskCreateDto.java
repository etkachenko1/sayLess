package com.sayless.task.dto;
import java.time.Instant;
public record TaskCreateDto(String title, String description, Instant deadline, String assignedTo) {}


