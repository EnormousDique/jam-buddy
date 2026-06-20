package ru.muwa.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import ru.muwa.dto.InviteEvent;

// @Service
@Slf4j
@RequiredArgsConstructor
public class InviteConsumer {

    private final NotificationService service;

    @KafkaListener(
            topics = "invite-events",
            groupId = "tg-bot-invites-group",
            containerFactory = "KafkaListenerContainerFactory"
    )
    public void consume(InviteEvent event){

        log.info("Получено сообщение kafka: {}", event);

        String message = "Привет! Тебе пришло новое приглашение,";

        // TODO: отправка сообщений в телеграм чат получателя приглашения


    }
}
