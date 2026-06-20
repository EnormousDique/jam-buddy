package ru.muwa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.muwa.dto.InviteUpdateRequest;
import ru.muwa.dto.InvitesListResponse;
import ru.muwa.entity.InviteStatus;
import ru.muwa.service.InviteService;
import ru.muwa.service.SseService;

import java.util.UUID;

@RestController
@RequestMapping("/profiles/invite")
@RequiredArgsConstructor
@Slf4j
public class InviteController {

    private final InviteService inviteService;

    @PostMapping("/{receiverId}")
    public ResponseEntity<?> send(@PathVariable UUID receiverId, Authentication auth ){

        UUID senderUserId = (UUID) auth.getPrincipal();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        inviteService.send(senderUserId,receiverId,role);
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{inviteId}")
    public ResponseEntity<?> delete(@PathVariable UUID inviteId){

        inviteService.delete(inviteId);
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{inviteId}")
    public ResponseEntity<?> update(@PathVariable UUID inviteId, @RequestBody InviteUpdateRequest request) {

        InviteStatus status = request.getStatus();
        inviteService.update(inviteId, status);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/incoming")
    public InvitesListResponse incomingInvitesList(Authentication auth){
        return InvitesListResponse.builder()
                .list(inviteService.getIncomingInvites((UUID) auth.getPrincipal()))
                .build();
    }

    @GetMapping("/outgoing")
    public InvitesListResponse outgoingInvitesList(Authentication auth){
        return InvitesListResponse.builder()
                .list(inviteService.getOutgoingInvites((UUID) auth.getPrincipal()))
                .build();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication auth){
        log.info("вызов метода контроллера подписок на sse");
        UUID userId = (UUID) auth.getPrincipal();
        return inviteService.subscribeToInviteUpdates(userId);
    }

}
