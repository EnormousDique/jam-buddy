package ru.muwa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.muwa.dto.SearchRequest;
import ru.muwa.dto.SearchResponse;
import ru.muwa.service.ProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/profiles/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request,
                                                 Authentication auth){

        return ResponseEntity.ok(profileService.search(request, (UUID) auth.getPrincipal()));
    }
}
