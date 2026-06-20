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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.muwa.dto.InviteUpdateRequest;
import ru.muwa.entity.InviteStatus;
import ru.muwa.service.InviteService;
import ru.muwa.service.JwtService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InviteController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class InviteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InviteService inviteService;

    @MockBean
    private JwtService jwtService; // Глушим фильтр

    private Authentication mockAuthentication;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(userId);
    }

    // ==========================================
    // ТЕСТ POST /profiles/invite/{receiverId}
    // ==========================================
    @Test
    void send_ShouldReturnOk_WhenInviteSent() throws Exception {
        UUID receiverId = UUID.randomUUID();
        String role = "ROLE_USER";

        // Настраиваем получение роли из authorities объекта Authentication
        GrantedAuthority authority = new SimpleGrantedAuthority(role);
        doReturn(Collections.singleton(authority)).when(mockAuthentication).getAuthorities();

        mockMvc.perform(post("/profiles/invite/{receiverId}", receiverId)
                        .principal(mockAuthentication))
                .andExpect(status().isOk());

        verify(inviteService, times(1)).send(userId, receiverId, role);
    }

    // ==========================================
    // ТЕСТ DELETE /profiles/invite/{inviteId}
    // ==========================================
    @Test
    void delete_ShouldReturnOk_WhenInviteDeleted() throws Exception {
        UUID inviteId = UUID.randomUUID();

        mockMvc.perform(delete("/profiles/invite/{inviteId}", inviteId))
                .andExpect(status().isOk());

        verify(inviteService, times(1)).delete(inviteId);
    }

    // ==========================================
    // ТЕСТ PATCH /profiles/invite/{inviteId}
    // ==========================================
    @Test
    void update_ShouldReturnOk_WhenInviteUpdated() throws Exception {
        UUID inviteId = UUID.randomUUID();
        InviteUpdateRequest request = new InviteUpdateRequest();
        request.setStatus(InviteStatus.ACCEPTED); // Предполагаем, что этот enum содержит ACCEPTED

        mockMvc.perform(patch("/profiles/invite/{inviteId}", inviteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(inviteService, times(1)).update(inviteId, InviteStatus.ACCEPTED);
    }

    // ==========================================
    // ТЕСТ GET /profiles/invite/incoming
    // ==========================================
    @Test
    void incomingInvitesList_ShouldReturnInvitesListResponse() throws Exception {
        when(inviteService.getIncomingInvites(userId)).thenReturn(List.of());

        mockMvc.perform(get("/profiles/invite/incoming")
                        .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list").isArray());

        verify(inviteService, times(1)).getIncomingInvites(userId);
    }

    // ==========================================
    // ТЕСТ GET /profiles/invite/outgoing
    // ==========================================
    @Test
    void outgoingInvitesList_ShouldReturnInvitesListResponse() throws Exception {
        when(inviteService.getOutgoingInvites(userId)).thenReturn(List.of());

        mockMvc.perform(get("/profiles/invite/outgoing")
                        .principal(mockAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list").isArray());

        verify(inviteService, times(1)).getOutgoingInvites(userId);
    }

    // ==========================================
    // ТЕСТ GET /profiles/invite/subscribe (SSE)
    // ==========================================
    @Test
    void subscribe_ShouldReturnSseEmitter() throws Exception {
        SseEmitter mockEmitter = new SseEmitter();
        when(inviteService.subscribeToInviteUpdates(userId)).thenReturn(mockEmitter);

        mockMvc.perform(get("/profiles/invite/subscribe")
                        .principal(mockAuthentication)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted()); // Проверяем, что Spring переключил запрос в асинхронный стрим

        verify(inviteService, times(1)).subscribeToInviteUpdates(userId);
    }
}