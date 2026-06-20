package ru.muwa.service;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.muwa.entity.Role;
import ru.muwa.entity.User;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // Устанавливаем секретный ключ (минимум 32 байта для HS256) и время жизни через рефлексию
        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, "mySuperSecretKeyForJwtTokenGeneration32BytesLong!");

        Field expirationField = JwtService.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, 3600000L); // 1 час в миллисекундах

        // Готовим тестового пользователя
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .role(Role.USER) // Если у тебя в Role используется Enum, это сработает. Если String - поменяй на строку.
                .build();
    }

    // ==========================================
    // ТЕСТ МЕТОДА generateToken()
    // ==========================================
    @Test
    void generateToken_ShouldReturnValidTokenString() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT токен состоит из трех частей, разделенных точками (header.payload.signature)
        assertEquals(3, token.split("\\.").length);
    }

    // ==========================================
    // ТЕСТ МЕТОДА extractUserId()
    // ==========================================
    @Test
    void extractUserId_ShouldReturnCorrectUuid() {
        String token = jwtService.generateToken(testUser);

        UUID extractedId = jwtService.extractUserId(token);

        assertNotNull(extractedId);
        assertEquals(userId, extractedId);
    }

    // ==========================================
    // ТЕСТ МЕТОДА extractRole()
    // ==========================================
    @Test
    void extractRole_ShouldReturnCorrectRoleName() {
        String token = jwtService.generateToken(testUser);

        String extractedRole = jwtService.extractRole(token);

        assertNotNull(extractedRole);
        assertEquals("USER", extractedRole);
    }

    // ==========================================
    // ТЕСТЫ МЕТОДА isTokenValid()
    // ==========================================
    @Test
    void isTokenValid_ValidToken_ShouldReturnTrue() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_InvalidOrTamperedToken_ShouldReturnFalse() {
        String token = jwtService.generateToken(testUser);
        // Ломаем токен, заменяя символы в его сигнатуре
        String tamperedToken = token + "corrupted";

        boolean isValid = jwtService.isTokenValid(tamperedToken);

        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ExpiredToken_ShouldReturnFalse() throws Exception {
        // Устанавливаем отрицательное время жизни токена, чтобы он мгновенно стал протухшим
        Field expirationField = JwtService.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, -1000L); // -1 секунда

        String expiredToken = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(expiredToken);

        assertFalse(isValid);
    }
}