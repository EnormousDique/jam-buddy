package ru.muwa.dto;

import lombok.Builder;
import lombok.Data;
import ru.muwa.entity.Profile;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {

    Profile profile;

}

