package ru.muwa.dto;

import lombok.*;

import java.util.Set;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private Double longitude;
    private Double latitude;
    private Double radius;
    private Integer minAge;
    private Integer maxAge;
    private String gender;
    private Set<Integer> instrumentIds;
}
