package ru.muwa.dto;

import jakarta.persistence.*;
import lombok.*;
import ru.muwa.entity.Instrument;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDto {

    private UUID id;

    private UUID userId;

    private String name;
    private Integer age;
    private String gender;

    private String description;

    private Double latitude;
    private Double longitude;

    private Set<Instrument> instruments = new HashSet<>();

    private Instant createdAt;
}
