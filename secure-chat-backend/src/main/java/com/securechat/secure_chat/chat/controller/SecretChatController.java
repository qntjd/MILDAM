package com.securechat.secure_chat.chat.controller;

import com.securechat.secure_chat.chat.dto.SecretMessageRequest;
import com.securechat.secure_chat.chat.service.SecretChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SecretChatController {

    private final SecretChatService secretChatService;

    // ── REST ──────────────────────────────────────────────────────

    /** 시크릿 채팅 세션 시작 */
    @PostMapping("/api/secret/init")
    @ResponseBody
    public ResponseEntity<Map<String, String>> initSession(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String recipientId = body.get("recipientId");
        if (recipientId == null || recipientId.isBlank()) {
            throw new IllegalArgumentException("상대방 ID는 필수입니다");
        }

        String sessionId = secretChatService.initSession(
                authentication.getName(), recipientId);

        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /** 메시지 양방향 삭제 */
    @DeleteMapping("/api/secret/{sessionId}/messages/{messageId}")
    @ResponseBody
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String sessionId,
            @PathVariable String messageId) {

        secretChatService.deleteMessage(sessionId, messageId);
        return ResponseEntity.noContent().build();
    }

    /** 세션 종료 */
    @DeleteMapping("/api/secret/{sessionId}")
    @ResponseBody
    public ResponseEntity<Void> terminateSession(
            @PathVariable String sessionId) {

        secretChatService.terminateSession(sessionId, "USER_REQUEST");
        return ResponseEntity.noContent().build();
    }

    // ── WebSocket ─────────────────────────────────────────────────

    /** 공개키 중계 — 서버는 내용 모름 */
    @MessageMapping("/secret.key")
    public void relayPublicKey(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        secretChatService.relayPublicKey(
                payload.get("sessionId"),
                auth.getName(),
                payload.get("publicKey"));
    }

    /** Key Fingerprint 확인 */
    @MessageMapping("/secret.confirm")
    public void confirmKeyExchange(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        secretChatService.confirmKeyExchange(
                payload.get("sessionId"),
                auth.getName(),
                payload.get("fingerprintEmoji"));
    }

    /** 암호화 메시지 중계 — 서버는 복호화 불가 */
    @MessageMapping("/secret.send")
    public void sendSecretMessage(
            @Valid @Payload SecretMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        secretChatService.relayEncryptedMessage(
                request.getSessionId(),
                auth.getName(),
                request.getCipherText(),
                request.getTtlSeconds());
    }
}