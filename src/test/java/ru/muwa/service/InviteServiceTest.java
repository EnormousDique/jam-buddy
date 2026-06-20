package ru.muwa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.muwa.constant.InviteLimits;
import ru.muwa.constant.UserRole;
import ru.muwa.dto.InviteDto;
import ru.muwa.entity.Invite;
import ru.muwa.entity.InviteStatus;
import ru.muwa.entity.OutboxEvent;
import ru.muwa.entity.Profile;
import ru.muwa.exceptions.OutOfDailyInviteLimitException;
import ru.muwa.repository.InviteRepository;
import ru.muwa.repository.OutboxRepository;
import ru.muwa.repository.ProfileRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SseService sseService;

    @InjectMocks
    private InviteService inviteService;

    private UUID userId;
    private UUID senderProfileId;
    private UUID receiverProfileId;
    private Profile senderProfile;
    private Profile receiverProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        senderProfileId = UUID.randomUUID();
        receiverProfileId = UUID.randomUUID();

        senderProfile = Profile.builder()
                .id(senderProfileId)
                .name("Sender Musician")
                .age(25)
                .description("Guitarist")
                .instruments(new HashSet<>())
                .build();

        receiverProfile = Profile.builder()
                .id(receiverProfileId)
                .name("Receiver Musician")
                .age(30)
                .description("Drummer")
                .telegramUsername("@receiver_tg")
                .instruments(new HashSet<>())
                .build();

    }

    // ==========================================
    // ТЕСТЫ МЕТОДА send()
    // ==========================================

    @Test
    void send_Success_AsUser() throws JsonProcessingException {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(senderProfile));
        when(inviteRepository.countDailyInvites(eq(userId), any(LocalDateTime.class))).thenReturn(2L); // 2 < 5
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertDoesNotThrow(() -> inviteService.send(userId, receiverProfileId, UserRole.USER));

        verify(inviteRepository, times(1)).save(any(Invite.class));
        verify(sseService, times(1)).sendNotification(receiverProfileId);
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void send_ThrowsOutOfDailyInviteLimitException_ForUser() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(senderProfile));
        when(inviteRepository.countDailyInvites(eq(userId), any(LocalDateTime.class))).thenReturn(5L); // Лимит достигнут

        assertThrows(OutOfDailyInviteLimitException.class,
                () -> inviteService.send(userId, receiverProfileId, UserRole.USER));

        verify(inviteRepository, never()).save(any(Invite.class));
        verify(sseService, never()).sendNotification(any());
    }

    @Test
    void send_ProfileNotFound_ThrowsRuntimeException() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inviteService.send(userId, receiverProfileId, UserRole.USER));

        assertEquals("Профиль не найден", exception.getMessage());
    }

    // ==========================================
    // ТЕСТЫ МЕТОДОВ ПОЛУЧЕНИЯ ИНВАЙТОВ
    // ==========================================

    @Test
    void getOutgoingInvites_Success() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(senderProfile));

        Invite invite = Invite.builder()
                .id(UUID.randomUUID())
                .senderId(senderProfileId)
                .receiverId(receiverProfileId)
                .status(InviteStatus.PENDING)
                .build();

        when(inviteRepository.findBySenderId(senderProfileId)).thenReturn(List.of(invite));
        when(profileRepository.findById(receiverProfileId)).thenReturn(Optional.of(receiverProfile));

        List<InviteDto> dtos = inviteService.getOutgoingInvites(userId);

        assertFalse(dtos.isEmpty());
        assertEquals(1, dtos.size());
        assertEquals(receiverProfileId, dtos.get(0).getReceiverId());
        assertEquals("Receiver Musician", dtos.get(0).getReceiverScreenName());
    }

    @Test
    void getIncomingInvites_Success_AcceptedStatus_ShowsTelegram() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(receiverProfile)); // мы в роли получателя

        Invite invite = Invite.builder()
                .id(UUID.randomUUID())
                .senderId(senderProfileId)
                .receiverId(receiverProfileId)
                .status(InviteStatus.ACCEPTED) // Статус ПРИНЯТ
                .build();

        when(inviteRepository.findByReceiverId(receiverProfileId)).thenReturn(List.of(invite));
        when(profileRepository.findById(senderProfileId)).thenReturn(Optional.of(senderProfile));

        List<InviteDto> dtos = inviteService.getIncomingInvites(userId);

        assertFalse(dtos.isEmpty());
        assertEquals(InviteStatus.ACCEPTED, dtos.get(0).getStatus());
    }

    // ==========================================
    // ТЕСТЫ МЕТОДОВ ДЕЙСТВИЙ (delete, update, subscribe)
    // ==========================================

    @Test
    void delete_Success() {
        UUID inviteId = UUID.randomUUID();
        Invite invite = Invite.builder()
                .id(inviteId)
                .senderId(senderProfileId)
                .receiverId(receiverProfileId)
                .build();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        inviteService.delete(inviteId);

        verify(inviteRepository, times(1)).deleteById(inviteId);
        verify(sseService, times(1)).sendNotification(senderProfileId);
        verify(sseService, times(1)).sendNotification(receiverProfileId);
    }

    @Test
    void update_Success() {
        UUID inviteId = UUID.randomUUID();
        Invite invite = Invite.builder()
                .id(inviteId)
                .senderId(senderProfileId)
                .receiverId(receiverProfileId)
                .status(InviteStatus.PENDING)
                .build();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inviteService.update(inviteId, InviteStatus.ACCEPTED);

        assertEquals(InviteStatus.ACCEPTED, invite.getStatus());
        verify(sseService, times(1)).sendNotification(senderProfileId);
        verify(sseService, times(1)).sendNotification(receiverProfileId);
    }

    @Test
    void subscribeToInviteUpdates_Success() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(senderProfile));
        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(sseService.subscribe(senderProfileId)).thenReturn(mockEmitter);

        SseEmitter result = inviteService.subscribeToInviteUpdates(userId);

        assertNotNull(result);
        assertEquals(mockEmitter, result);
        verify(sseService, times(1)).subscribe(senderProfileId);
    }
}