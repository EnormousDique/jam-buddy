package ru.muwa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.muwa.constant.InviteLimits;
import ru.muwa.constant.InviteOutboxEventTypes;
import ru.muwa.constant.UserRole;
import ru.muwa.dto.InviteDto;
import ru.muwa.dto.InviteEventPayload;
import ru.muwa.entity.OutboxEvent;
import ru.muwa.entity.Invite;
import ru.muwa.entity.InviteStatus;
import ru.muwa.entity.Profile;
import ru.muwa.exceptions.OutOfDailyInviteLimitException;
import ru.muwa.repository.InviteRepository;
import ru.muwa.repository.OutboxRepository;
import ru.muwa.repository.ProfileRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final ProfileRepository profileRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final SseService sseService;

    @Transactional
    public void send(UUID senderId, UUID receiverId, String role) {

        Profile sender = profileRepository.findByUserId(senderId)
                .orElseThrow(() -> new RuntimeException("Профиль не найден"));

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        long sentToday = inviteRepository.countDailyInvites(senderId,dayStart);

        if(
                (role.equals(UserRole.USER) && sentToday >= InviteLimits.USER_LIMIT)
                ||
                (role.equals(UserRole.PREMIUM) && sentToday >= InviteLimits.PREMIUM_LIMIT)
        )
            throw new OutOfDailyInviteLimitException("Превышено количество заявок на сегодня");

        inviteRepository.save(Invite.builder()
                        .id(UUID.randomUUID())
                        .createdAt(LocalDateTime.now())
                        .senderId(sender.getId())
                        .receiverId(receiverId)
                        .status(InviteStatus.PENDING)
                .build());

        sseService.sendNotification(receiverId);

        try {
            outboxRepository.save(OutboxEvent.builder()
                    .type(InviteOutboxEventTypes.INVITE_CREATED)
                    .payload(objectMapper.writeValueAsString(InviteEventPayload.builder()
                            .inviteId(UUID.randomUUID())
                            .senderId(senderId)
                            .receiverId(receiverId)
                            .build()))
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (JsonProcessingException ex) { throw new RuntimeException(ex.getMessage());}
    }

    public List<InviteDto> getOutgoingInvites(UUID id) {

        // Отправитель - это наш юзер, отправивший запрос
        Profile sender = profileRepository.findByUserId(id)
                .orElseThrow(() -> new RuntimeException("Не найдена анкета пользователя при поиске приглашений."));

        // Смотрим его приглашения
        List<Invite> invites = inviteRepository.findBySenderId(sender.getId());

        if(invites.isEmpty()) return new LinkedList<>();

        // В каждом приглашении
        List<InviteDto> dtos =  invites.stream()
                .map(i -> {

                            // Смотрим кому он его отправил
                            Profile receiver = profileRepository.findById(i.getReceiverId())
                                    .orElseThrow(() -> new RuntimeException("Не найдена анкета пользователя при поиске приглашений."));

                            // И собираем в дто
                            return InviteDto.builder()
                                    .id(i.getId())
                                    .instruments(receiver.getInstruments())
                                    .receiverId(receiver.getId())
                                    .receiverScreenName(receiver.getName())
                                    .receiverProfileDescription(receiver.getDescription())
                                    .status(i.getStatus())
                                    .receiverAge(receiver.getAge())
                                    .receiverTelegramUsername(i.getStatus().equals(InviteStatus.ACCEPTED)? receiver.getTelegramUsername():"")
                                    .build();
                            }

                ).toList();

        return dtos;
    }


    public List<InviteDto> getIncomingInvites(UUID id) {

        // Получатель это наш пользователь, инициатор запроса
        Profile receiver = profileRepository.findByUserId(id)
                .orElseThrow(() -> new RuntimeException("Не найдена анкета пользователя при поиске приглашений."));

        // Смотрим его приглашения
        List<Invite> invites = inviteRepository.findByReceiverId(receiver.getId());

        if(invites.isEmpty()) return new LinkedList<>();

        // В каждом приглашении
        List<InviteDto> dtos = invites.stream()
                .map(i -> {

                    // Смотрим кто его пригласил
                    Profile sender = profileRepository.findById(i.getSenderId())
                            .orElseThrow(() -> new RuntimeException("Не найдена анкета пользователя при поиске приглашений."));

                    // И собираем данные в дто
                    return InviteDto.builder()
                            .id(i.getId())
                            .instruments(sender.getInstruments())
                            .senderId(sender.getId())
                            .senderScreenName(sender.getName())
                            .senderProfileDescription(sender.getDescription())
                            .status(i.getStatus())
                            .senderAge(sender.getAge())
                            .senderTelegramUsername(i.getStatus().equals(InviteStatus.ACCEPTED)? sender.getTelegramUsername():"")
                            .build();
                    }
                ).toList();

        return dtos;
    }

    public void delete(UUID id) {
        // TODO: может вынести в метод ссе сервиса эту логику?
        Invite invite = inviteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Не найдено приглашение для удаления"));
        inviteRepository.deleteById(id); // КРОМЕ ЭТОГО
        sseService.sendNotification(invite.getSenderId());
        sseService.sendNotification(invite.getReceiverId());
    }

    @Transactional
    public void update(UUID id, InviteStatus status) {

        Invite invite =
        inviteRepository.findById(id)
                .map(i -> {
                    i.setStatus(status);
                    return
                    inviteRepository.save(i);
                })
                .orElseThrow(
                        () -> new
                                RuntimeException
                                ("не найдено приглашение для отмены"));

        sseService.sendNotification(invite.getSenderId());
        sseService.sendNotification(invite.getReceiverId());
    }

    public SseEmitter subscribeToInviteUpdates(UUID userId){
        UUID profileId = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Не найдена анкета музыканта для создания подписок на события приглашений."))
                .getId();
        return sseService.subscribe(profileId);
    }

}
