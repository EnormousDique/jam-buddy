package ru.muwa.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.muwa.dto.ProfileRequest;
import ru.muwa.dto.ProfileResponse;
import ru.muwa.dto.SearchRequest;
import ru.muwa.dto.SearchResponse;
import ru.muwa.entity.Profile;
import ru.muwa.mapper.ProfileMapper;
import ru.muwa.repository.InstrumentRepository;
import ru.muwa.repository.ProfileRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private ProfileMapper mapper;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private UUID profileId;
    private ProfileRequest profileRequest;
    private Profile existingProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileId = UUID.randomUUID();

        profileRequest = new ProfileRequest();
        profileRequest.setName("John Doe");
        profileRequest.setAge(28);
        profileRequest.setGender("MALE");
        profileRequest.setLatitude(55.7558);
        profileRequest.setLongitude(37.6173);
        profileRequest.setDescription("Guitar player");
        profileRequest.setTelegramUsername("@johndoe");
        profileRequest.setInstruments(Set.of(1, 2));

        existingProfile = Profile.builder()
                .id(profileId)
                .userId(userId)
                .name("Old Name")
                .age(25)
                .instruments(new HashSet<>())
                .build();
    }

    // ==========================================
    // ТЕСТЫ МЕТОДА saveProfile()
    // ==========================================

    @Test
    void saveProfile_CreateNewProfile_Success() {
        // Настраиваем мок: профиль не найден, значит сработает ветка создания (createProfile)
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(instrumentRepository.findAllById(profileRequest.getInstruments())).thenReturn(Collections.emptyList());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Profile savedProfile = profileService.saveProfile(profileRequest, userId);

        assertNotNull(savedProfile);
        assertEquals(userId, savedProfile.getUserId());
        assertEquals("John Doe", savedProfile.getName());
        assertEquals(28, savedProfile.getAge());
        assertNotNull(savedProfile.getId()); // ID должен сгенерироваться внутри метода

        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    void saveProfile_UpdateExistingProfile_Success() {
        // Настраиваем мок: профиль найден, значит сработает ветка обновления (updateProfile)
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(instrumentRepository.findAllById(profileRequest.getInstruments())).thenReturn(Collections.emptyList());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Profile savedProfile = profileService.saveProfile(profileRequest, userId);

        assertNotNull(savedProfile);
        assertEquals(profileId, savedProfile.getId()); // ID не должен измениться
        assertEquals("John Doe", savedProfile.getName()); // Имя должно обновиться
        assertEquals(28, savedProfile.getAge());

        verify(profileRepository, times(1)).save(existingProfile);
    }

    // ==========================================
    // ТЕСТЫ МЕТОДА get()
    // ==========================================

    @Test
    void get_Success() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(existingProfile));
        ProfileResponse mockResponse = ProfileResponse.builder().profile(existingProfile).build();
        when(mapper.toResponse(existingProfile)).thenReturn(mockResponse);

        ProfileResponse response = profileService.get(profileId);

        assertNotNull(response);
        verify(profileRepository, times(1)).findById(profileId);
    }

    @Test
    void get_NotFound_ThrowsRuntimeException() {
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> profileService.get(profileId));
        assertEquals("Профиль не найден", exception.getMessage());
    }

    // ==========================================
    // ТЕСТ МЕТОДА getByUserId()
    // ==========================================

    @Test
    void getByUserId_ProfileExists() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        ProfileResponse response = profileService.getByUserId(userId);

        assertNotNull(response);
        assertEquals(existingProfile, response.getProfile());
    }

    @Test
    void getByUserId_ProfileDoesNotExist_ReturnsResponseWithNullProfile() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ProfileResponse response = profileService.getByUserId(userId);

        assertNotNull(response);
        assertNull(response.getProfile());
    }

    // ==========================================
    // ТЕСТ МЕТОДА search()
    // ==========================================

    @Test
    void search_Success() {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setLatitude(55.7558);
        searchRequest.setLongitude(37.6173);
        searchRequest.setRadius(10.0);
        searchRequest.setGender("MALE");
        searchRequest.setMinAge(18);
        searchRequest.setMaxAge(40);
        searchRequest.setInstrumentIds(Set.of(1));

        when(profileRepository.findNearby(
                eq(55.7558), eq(37.6173), eq(10.0), eq("MALE"), eq(18), eq(40),
                eq(Set.of(1)), eq(1), eq(userId)
        )).thenReturn(List.of(existingProfile));

        SearchResponse response = profileService.search(searchRequest, userId);

        assertNotNull(response);
        assertNotNull(response.getProfiles());
        verify(profileRepository, times(1)).findNearby(
                anyDouble(), anyDouble(), anyDouble(), anyString(), anyInt(), anyInt(),
                anySet(), anyInt(), any(UUID.class)
        );
    }
}