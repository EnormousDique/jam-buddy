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
import ru.muwa.dto.SearchRequest;
import ru.muwa.dto.SearchResponse;
import ru.muwa.service.JwtService;
import ru.muwa.service.ProfileService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class SearchControllerTest {

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

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(userId);
    }

    @Test
    void search_ShouldReturnOkAndSearchResponse() throws Exception {
        // Готовим тело запроса
        SearchRequest request = new SearchRequest();
        // Если внутри SearchRequest есть поля (например, радиус поиска или теги),
        // при желании можно их здесь инициализировать.

        // Задаем ожидаемый ответ от сервиса.
        // Предполагаем, что у SearchResponse есть билдер и, например, поле списка найденных анкет/профилей.
        SearchResponse mockResponse = SearchResponse.builder()
                // .profiles(Collections.emptyList()) // Настрой под реальные поля твоего SearchResponse DTO
                .build();

        when(profileService.search(any(SearchRequest.class), eq(userId)))
                .thenReturn(mockResponse);

        // Выполняем POST-запрос
        mockMvc.perform(post("/profiles/search")
                        .principal(mockAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // Проверяем, что ResponseEntity.ok() вернул 200

        // Проверяем, что метод сервиса вызвался с нужными параметрами
        verify(profileService, times(1)).search(any(SearchRequest.class), eq(userId));
    }
}