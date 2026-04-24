package com.securechat.secure_chat.chat.dto;

import com.securechat.secure_chat.domain.message.Message;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class MessageResponse {

    private final UUID messageId;
    private final UUID roomId;
    private final String senderUsername;
    private final String content;
    private final Instant createdAt;
    private final Instant expiresAt;

    public MessageResponse(Message message, String decryptedContent) {
        this.messageId       = message.getId();
        this.roomId          = message.getRoom().getId();
        this.senderUsername  = message.getSender().getUsername();
        this.content         = decryptedContent;
        this.createdAt       = message.getCreatedAt();
        this.expiresAt       = message.getExpiresAt();
    }
}