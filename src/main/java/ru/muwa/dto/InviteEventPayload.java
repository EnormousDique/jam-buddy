package ru.muwa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InviteEventPayload {
    private UUID inviteId;
    private UUID senderId;
    private UUID receiverId;
}
