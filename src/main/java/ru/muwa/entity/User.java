package ru.muwa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(unique = true)
    private String telegramUsername;

    @Column(unique = true)
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Instant createdAt;
}
