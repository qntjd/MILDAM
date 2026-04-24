package com.securechat.secure_chat.chat.controller;

import com.securechat.secure_chat.chat.service.MessageReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageReadController {

    private final MessageReadService messageReadService;

    @MessageMapping("/message.read")
    public void markAsRead(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        UUID messageId = UUID.fromString(payload.get("messageId"));
        messageReadService.markAsRead(messageId, auth.getName());
    }

    @PostMapping("/api/rooms/{roomId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable UUID roomId,
            Authentication authentication) {

        messageReadService.markAllAsRead(roomId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/messages/{messageId}/read-count")
    public ResponseEntity<Map<String, Long>> getReadCount(
            @PathVariable UUID messageId) {

        long count = messageReadService.getReadCount(messageId);
        return ResponseEntity.ok(Map.of("readCount", count));
    }
}