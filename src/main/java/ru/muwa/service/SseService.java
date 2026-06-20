package ru.muwa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final long SSE_EMITTER_TIMEOUT = 600_000L; // 10 min

    public SseEmitter subscribe(UUID profileId){

        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);

        emitter.onCompletion(() -> emitters.remove(profileId));
        emitter.onTimeout(() -> emitters.remove(profileId));
        emitter.onError((e) -> {
            emitters.remove(profileId);
            throw new RuntimeException("Ошибка эмиттера событий сервера.");
        });

        emitters.put(profileId,emitter);
        log.info("Подписка на обновления приглашений активирована для анкеты: " + profileId);

        return emitter;
    }

    public void sendNotification(UUID profileId){

        SseEmitter emitter = emitters.get(profileId);

        if(emitter != null)
            try {
                emitter.send(SseEmitter.event().data("update"));
                log.info("Отправлено sse событие");
            } catch (Exception e) {
                log.error("");
                emitters.remove(profileId);
            }
    }

}
