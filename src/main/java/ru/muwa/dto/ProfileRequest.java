package ru.muwa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class ProfileRequest {

    @NotBlank
    private String name;

    private Integer age;
    private String gender;
    private String description;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private Set<Integer> instruments;

    @NotNull
    private String telegramUsername;
}

