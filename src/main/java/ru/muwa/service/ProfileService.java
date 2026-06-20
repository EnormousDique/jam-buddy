package ru.muwa.service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.muwa.dto.ProfileRequest;
import ru.muwa.dto.ProfileResponse;
import ru.muwa.dto.SearchRequest;
import ru.muwa.dto.SearchResponse;
import ru.muwa.entity.Profile;
import ru.muwa.mapper.ProfileMapper;
import ru.muwa.repository.InstrumentRepository;
import ru.muwa.repository.ProfileRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final InstrumentRepository instrumentRepository;
    private final ProfileMapper mapper;

    @Transactional
    public Profile saveProfile(ProfileRequest dto, UUID userId){

        Profile profile = profileRepository.findByUserId(userId)
                .map(existing -> updateProfile(existing,dto))
                .orElseGet(() -> createProfile(dto,userId));

        return profileRepository.save(profile);
    }

    private Profile createProfile(ProfileRequest dto, UUID userId) {
        return Profile.builder()
                .createdAt(Instant.now())
                .name(dto.getName())
                .id(UUID.randomUUID())
                .age(dto.getAge())
                .longitude(dto.getLongitude())
                .latitude(dto.getLatitude())
                .userId(userId)
                .gender(dto.getGender())
                .instruments(new HashSet<>( instrumentRepository.findAllById(dto.getInstruments())))
                .description(dto.getDescription())
                .telegramUsername(dto.getTelegramUsername())
                .build();
    }

    private Profile updateProfile(Profile existing, ProfileRequest dto) {
        existing.setAge(dto.getAge());
        existing.setDescription(dto.getDescription());
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());
        existing.setName(dto.getName());
        existing.setGender(dto.getGender());
        existing.setDescription(dto.getDescription());
        existing.setInstruments(new HashSet<>(instrumentRepository.findAllById(dto.getInstruments())));
        return existing;
    }

    public ProfileResponse get(UUID id) {
        return mapper.toResponse(
                profileRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Профиль не найден"))
        );
    }

    public ProfileResponse getByUserId(UUID uuid) {
        return ProfileResponse.builder()
                .profile(
                        profileRepository.findByUserId(uuid).orElse(null))
                .build();
    }

    public SearchResponse search(@Valid SearchRequest request, UUID userId) {
        return SearchResponse.builder()
                        .profiles(profileRepository.findNearby(
                                request.getLatitude(),
                                request.getLongitude(),
                                request.getRadius(),
                                request.getGender(),
                                request.getMinAge(),
                                request.getMaxAge(),
                                request.getInstrumentIds(),
                                request.getInstrumentIds().size(),
                                userId)
                                .stream()
                                .map(mapper::toDto)
                                .toList()
                        )
                        .build();
    }
}
