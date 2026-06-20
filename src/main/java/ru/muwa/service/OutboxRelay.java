package ru.muwa.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.muwa.entity.OutboxEvent;
import ru.muwa.repository.OutboxRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutbox() {

        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

        for(var e : events){
            try {
                kafkaTemplate.send("invite-events", e.getPayload());
                e.setProcessed(true);
                outboxRepository.save(e);
            } catch (Exception ex) {
                log.error("Ошибка при отправки сообщения в kafka в рамках outbox. \n {}: {}",e.getId(),ex.getMessage());
            }
        }

    }

}
