package ru.muwa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.muwa.dto.AuthResponse;
import ru.muwa.dto.LoginRequest;
import ru.muwa.dto.RegisterRequest;
import ru.muwa.dto.TelegramUser;
import ru.muwa.entity.Role;
import ru.muwa.entity.User;
import ru.muwa.repository.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;


    @Value("${bot.token}")
    private String botToken;


    public AuthResponse register(RegisterRequest request){

        if(repository.existsByTelegramId(request.getTelegramId())){
            return null;
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .telegramId(request.getTelegramId())
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        repository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .build();
    }

    public AuthResponse login(LoginRequest request){

        User user = repository.findByTelegramUsername(request.getTelegramUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден")); // TODO: ЗАМЕНИТЬ НА БИЗНЕС-ЛОГИКУ-ИСКЛЮЧЕНИЕ

        // Пароль не требуется. Клиент обращается из телеграм, соответственно он уже ввел пароль при входе туда
       // if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
       //     throw new RuntimeException("Invalid credentials");
       // }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .build();
    }


    public AuthResponse authenticateTelegram(String initData){

        log.info("authenticateTelegram()");

        if(!isTelegramInitDataValid(initData,botToken))
            throw new BadCredentialsException("Недействительные telegram initData");


        TelegramUser tgUser = parseTelegramUser(initData);

        User user = repository.findByTelegramId(tgUser.getId())
                .orElseGet(() -> {
                   User u = new User();
                   u.setId(UUID.randomUUID());
                   u.setTelegramId(tgUser.getId());
                   u.setRole(Role.USER);
                   u.setCreatedAt(Instant.now());
                   return repository.save(u);
                });

        String jwt = jwtService.generateToken(user);
        log.info("сгенерирован jwt");
        return AuthResponse.builder()
                .accessToken(jwt)
                .build();
    }

    private TelegramUser parseTelegramUser(String initData) {
        try {
            String userJson = Arrays.stream(initData.split("&"))
                    .filter(param -> param.startsWith("user="))
                    .map(param -> param.substring(5))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Отсутствует пользователь в initData"));

            String decodedJson = URLDecoder.decode(userJson, StandardCharsets.UTF_8);

            return objectMapper.readValue(decodedJson, TelegramUser.class);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга данных пользователя Telegram", e);
        }
    }


    private boolean isTelegramInitDataValid(String initData, String botToken) {
        try {
            Map<String, String> map = new HashMap<>();
            for (String s : initData.split("&")) {
                String[] kv = s.split("=", 2);
                map.put(kv[0], kv[1]);
            }

            String hash = map.remove("hash");
            if (hash == null) return false;

            String dataCheckString = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + URLDecoder.decode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("\n"));

            byte[] secretKey = hmacSha256(botToken, "WebAppData".getBytes(StandardCharsets.UTF_8));
            byte[] finalHash = hmacSha256(dataCheckString, secretKey);
            String calculatedHash = HexFormat.of().formatHex(finalHash);

            log.info("Calculated Hash: {}", calculatedHash);
            log.info("Received Hash: {}", hash);

            return calculatedHash.equals(hash);
        } catch (Exception e) {
            log.error("Validation error", e);
            return false;
        }
    }

    private byte[] hmacSha256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

}
