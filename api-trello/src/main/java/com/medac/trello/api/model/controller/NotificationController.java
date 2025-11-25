package com.medac.trello.api.model.controller;

import com.medac.trello.api.dto.NotificationResponseDTO;
import com.medac.trello.api.model.User;
import com.medac.trello.api.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/notificaciones", produces = APPLICATION_JSON_VALUE)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponseDTO> listarTodasLasNotificaciones(@AuthenticationPrincipal User authenticatedUser) {
        return notificationService.findAllNotificationsForUser(authenticatedUser).stream()
                .map(NotificationResponseDTO::new)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity borrarNotificacion(@PathVariable("id") Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }
}
