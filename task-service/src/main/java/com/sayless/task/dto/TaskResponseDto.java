package com.sayless.task.dto;
import java.time.Instant;
public record TaskResponseDto (
    String id, 
    String title, 
    String description, 
    String status,
    Instant deadline,
    String createdById,
    String createdByName,
    String assignedToId,
    String assignedToName
    
){}

