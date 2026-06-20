package ru.muwa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.muwa.dto.ProfileRequest;
import ru.muwa.dto.ProfileResponse;
import ru.muwa.service.ProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    @PostMapping
    public ProfileResponse create(@Valid @RequestBody ProfileRequest request,
                                  Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        return ProfileResponse.builder()
                .profile(service.saveProfile(request, userId))
                .build();

    }

    /*
    @GetMapping("/{id}")
    public ProfileResponse get(@PathVariable UUID id) {
        return service.get(id);
    } // Получаем по profile uuid
     */

    @GetMapping("/my")
    public ProfileResponse my(Authentication authentication){
            return service.getByUserId( (UUID) authentication.getPrincipal());
    } // Получаем по user uuid из афентикации
}
