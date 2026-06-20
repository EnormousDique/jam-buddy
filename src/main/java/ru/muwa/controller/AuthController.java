package ru.muwa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.muwa.dto.AuthResponse;
import ru.muwa.dto.LoginRequest;
import ru.muwa.dto.RegisterRequest;
import ru.muwa.service.AuthService;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    public UUID me(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    @PostMapping(value = "/telegram")
    public AuthResponse authTelegram(@RequestBody String initData){
        log.info("Произошел вызов /telegram в auth controller");
        log.info("Получены данные {}",initData);
        return service.authenticateTelegram(initData);
    }

}
