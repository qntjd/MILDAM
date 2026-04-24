package com.securechat.secure_chat.chat.controller;

import com.securechat.secure_chat.chat.dto.SendMessageRequest;
import com.securechat.secure_chat.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {

        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth == null) {
            throw new IllegalStateException("인증 정보가 없습니다");
        }

        messageService.sendMessage(auth.getName(), request);
    }

    @DeleteMapping("/api/messages/{messageId}")
    @ResponseBody
    public void deleteMessage(@PathVariable UUID messageId,
                              Authentication authentication) {
        messageService.deleteMessage(messageId, authentication.getName());
    }
}