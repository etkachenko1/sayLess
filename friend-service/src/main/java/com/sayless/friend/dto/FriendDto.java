package com.sayless.friend.dto;

import java.time.Instant;

public record FriendDto(
    String id,
    String requesterId,
    String requesterName,
    String receiverId,
    String receiverName,
    String status,
    Instant createdAt
) {}
