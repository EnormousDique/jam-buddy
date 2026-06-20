package ru.muwa.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.muwa.entity.Instrument;
import ru.muwa.repository.InstrumentRepository;
import ru.muwa.service.JwtService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Отключаем безопасность, чтобы тесты не спотыкались о фильтры
@WebMvcTest(controllers = InstrumentController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstrumentRepository instrumentRepository;

    // Глушим JwtService, если JwtAuthenticationFilter всё ещё сканируется Spring-контекстом
    @MockBean
    private JwtService jwtService;

    @Test
    void getInstruments_ShouldReturnOkAndInstrumentsList() throws Exception {
        // Создаем тестовые данные для репозитория
        Instrument guitar = new Instrument();
        guitar.setId(1);
        guitar.setName("Guitar");

        Instrument drums = new Instrument();
        drums.setId(2);
        drums.setName("Drums");

        List<Instrument> mockInstruments = List.of(guitar, drums);

        // Настраиваем поведение мока репозитория
        when(instrumentRepository.findAll()).thenReturn(mockInstruments);

        // Выполняем GET-запрос и проверяем структуру результирующего JSON
        mockMvc.perform(get("/profiles/instruments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instruments").isArray())
                .andExpect(jsonPath("$.instruments[0].id").value(1))
                .andExpect(jsonPath("$.instruments[0].name").value("Guitar"))
                .andExpect(jsonPath("$.instruments[1].id").value(2))
                .andExpect(jsonPath("$.instruments[1].name").value("Drums"));

        // Проверяем, что метод репозитория действительно дернулся ровно 1 раз
        verify(instrumentRepository, times(1)).findAll();
    }
}