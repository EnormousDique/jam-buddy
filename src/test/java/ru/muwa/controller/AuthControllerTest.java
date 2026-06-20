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
import ru.muwa.dto.AuthResponse;
import ru.muwa.dto.LoginRequest;
import ru.muwa.dto.RegisterRequest;
import ru.muwa.service.AuthService;
import ru.muwa.service.JwtService; // Импортируем твой JwtService

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Полностью глушим всю цепочку автоконфигурации безопасности на уровне фильтров сервлетов
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Закрываем дыру в зависимости JwtAuthenticationFilter constructor parameter 0
    @MockBean
    private JwtService jwtService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .accessToken("mocked-jwt-token")
                .build();
    }

    @Test
    void register_ShouldReturnOkAndAuthResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setTelegramId(12345L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void login_ShouldReturnOkAndAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setTelegramUsername("test_user");

        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void me_ShouldReturnUserId_WhenAuthenticated() throws Exception {
        UUID expectedUserId = UUID.randomUUID();

        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(expectedUserId);

        mockMvc.perform(get("/auth/me")
                        .principal(mockAuthentication))
                .andExpect(status().isOk())
                // Способ 1: Обернуть в кавычки строку, чтобы совпало с JSON-ответом:
                .andExpect(content().string("\"" + expectedUserId + "\""));

    }

    @Test
    void authTelegram_ShouldReturnOkAndAuthResponse() throws Exception {
        String initData = "query_id=AA...&user=...&hash=...";

        when(authService.authenticateTelegram(initData)).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"));

        verify(authService, times(1)).authenticateTelegram(initData);
    }
}