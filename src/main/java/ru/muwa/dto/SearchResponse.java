package ru.muwa.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.muwa.entity.Profile;

import java.util.List;

@Builder
@Getter
@Setter
public class SearchResponse {
    List<ProfileDto> profiles;
}
