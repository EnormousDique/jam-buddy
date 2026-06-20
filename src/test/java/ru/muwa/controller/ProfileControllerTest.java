package ru.muwa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import ru.muwa.dto.ProfileRequest;
import ru.muwa.dto.ProfileResponse;
import ru.muwa.service.JwtService;
import ru.muwa.service.ProfileService;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtService jwtService; // Глушим фильтр безопасности

    private Authentication mockAuthentication;
    private UUID userId;
    private ProfileResponse mockProfileResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(userId);

        mockProfileResponse = ProfileResponse.builder().build();
    }

    // ==========================================
    // ТЕСТ POST /profiles (Создание профиля)
    // ==========================================
    @Test
    void create_ShouldReturnOkAndProfileResponse_WhenRequestIsValid() throws Exception {
        // Заполняем все @NotBlank и @NotNull поля, чтобы пройти валидацию
        ProfileRequest request = new ProfileRequest();
        request.setName("Jam Buddy Musician");
        request.setLatitude(55.7558);
        request.setLongitude(37.6173);
        request.setTelegramUsername("jambuddy_user");

        // Необязательные поля тоже можно докинуть для полноты картины
        request.setAge(25);
        request.setGender("MALE");
        request.setDescription("Playing guitar and drums");
        request.setInstruments(Set.of(1, 2));

        // Настраиваем мок сервиса
        when(profileService.saveProfile(any(ProfileRequest.class), eq(userId)))
                .thenReturn(null);

        // Отправляем запрос
        mockMvc.perform(post("/profiles")
                        .principal(mockAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Проверяем вызов бизнес-логики
        verify(profileService, times(1)).saveProfile(any(ProfileRequest.class), eq(userId));
    }

    // ==========================================
    // ТЕСТ GET /profiles/my (Получение своего профиля)
    // ==========================================
    @Test
    void my_ShouldReturnProfileResponse() throws Exception {
        when(profileService.getByUserId(userId)).thenReturn(mockProfileResponse);

        mockMvc.perform(get("/profiles/my")
                        .principal(mockAuthentication))
                .andExpect(status().isOk());

        verify(profileService, times(1)).getByUserId(userId);
    }
}