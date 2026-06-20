package ru.muwa.entity;


import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String name;
    private Integer age;
    private String gender;

    @Column(length = 2000)
    private String description;

    private Double latitude;
    private Double longitude;

    @ManyToMany
    @JoinTable(
            name = "profile_instruments",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "instrument_id")
    )
    private Set<Instrument> instruments = new HashSet<>();

    private Instant createdAt;

    private String telegramUsername;
}
