package ru.muwa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.muwa.dto.ProfileDto;
import ru.muwa.dto.ProfileRequest;
import ru.muwa.dto.ProfileResponse;
import ru.muwa.entity.Instrument;
import ru.muwa.entity.Profile;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {Collectors.class, Instrument.class})
public interface ProfileMapper {

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "userId", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "instruments", ignore = true)
        Profile toEntity(ProfileRequest request);


        ProfileResponse toResponse(Profile profile);

        // @Mapping(target = "telegramUsername", ignore = true)
        ProfileDto toDto(Profile profile);
}