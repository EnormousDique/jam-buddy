package ru.muwa.dto;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InviteEvent {
    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private String status;
}
