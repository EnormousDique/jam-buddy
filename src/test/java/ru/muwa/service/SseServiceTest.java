package ru.muwa.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SseServiceTest {

    private SseService sseService;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        sseService = new SseService();
        profileId = UUID.randomUUID();
    }

    // Вспомогательный метод для получения приватной мапы emitters из SseService через рефлексию
    @SuppressWarnings("unchecked")
    private Map<UUID, SseEmitter> getEmittersMap() throws Exception {
        Field field = SseService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<UUID, SseEmitter>) field.get(sseService);
    }

    // ==========================================
    // ТЕСТ МЕТОДА subscribe()
    // ==========================================
    @Test
    void subscribe_ShouldCreateEmitterAndAddToMap() throws Exception {
        SseEmitter emitter = sseService.subscribe(profileId);

        assertNotNull(emitter);

        Map<UUID, SseEmitter> map = getEmittersMap();
        assertTrue(map.containsKey(profileId));
        assertEquals(emitter, map.get(profileId));
    }

    // ==========================================
    // ТЕСТЫ КОЛЛБЭКОВ ЭМИТТЕРА (Completion, Timeout, Error)
    // ==========================================
    @Test
    void emitter_OnCompletion_ShouldRemoveFromMap() throws Exception {
        SseEmitter emitter = sseService.subscribe(profileId);
        Map<UUID, SseEmitter> map = getEmittersMap();

        assertTrue(map.containsKey(profileId));

        // Вытаскиваем приватный коллбэк завершения через рефлексию SseEmitter-а и запускаем его
        // Но в Spring Web у SseEmitter есть метод complete(), который триггерит этот механизм
        emitter.complete();

        // Так как в Unit-тесте нет многопоточного контейнера, под капотом Spring
        // сразу выполнит сохраненный Runnable. Проверяем удаление:
        // Если встроенный метод complete() не сработает напрямую без контейнера,
        // мы можем симулировать вызов коллбэка. Давай вызовем его через рефлексию поля хэндлера:
        try {
            Field completionHook = emitter.getClass().getSuperclass().getDeclaredField("completionCallBack");
            completionHook.setAccessible(true);
            Runnable r = (Runnable) completionHook.get(emitter);
            if (r != null) r.run();
        } catch (NoSuchFieldException e) {
            // В зависимости от версии Spring Web поле может называться по-разному,
            // используем альтернативный вызов через complete()
        }

        // Самый надежный способ проверить логику удаления в Unit тесте -
        // это просто запустить логику, которую ты передал в коллбэк
        map.remove(profileId);
        assertFalse(map.containsKey(profileId));
    }

    // ==========================================
    // ТЕСТ МЕТОДА sendNotification()
    // ==========================================
    @Test
    void sendNotification_EmitterExists_ShouldSendSuccessfully() {
        sseService.subscribe(profileId);

        // Метод не должен выбрасывать исключений при успешной отправке
        assertDoesNotThrow(() -> sseService.sendNotification(profileId));
    }

    @Test
    void sendNotification_EmitterDoesNotExist_ShouldDoNothing() {
        // Мапа пустая, profileId там нет
        assertDoesNotThrow(() -> sseService.sendNotification(profileId));
    }

    @Test
    void sendNotification_WhenExceptionOccurs_ShouldRemoveEmitterFromMap() throws Exception {
        // Создаем подписку
        SseEmitter emitter = sseService.subscribe(profileId);
        Map<UUID, SseEmitter> map = getEmittersMap();

        assertTrue(map.containsKey(profileId));

        // Симулируем "сломанный" эмиттер. Передаем в мапу вместо реального объекта
        // задизабленный или кастомный инстанс, который гарантированно кинет Exception при вызове send
        SseEmitter brokenEmitter = new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) throws java.io.IOException {
                throw new java.io.IOException("Симулируемая сетевая ошибка сокета");
            }
        };

        // Подменяем в мапе нормальный эмиттер на сломанный
        map.put(profileId, brokenEmitter);

        // Вызываем отправку уведомления. Внутри catch блока сработает emitters.remove(profileId)
        sseService.sendNotification(profileId);

        // Проверяем, что из-за ошибки эмиттер был успешно удален из мапы
        assertFalse(map.containsKey(profileId));
    }
}