package com.securechat.secure_chat.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.securechat.secure_chat.domain.chat.ChatRoom;
import lombok.Getter;

@Getter
public class ChatRoomResponse {

    private final UUID id;
    private final String name;
    private final String type;
    private final String inviteCode;
    private final Instant createdAt;

    public ChatRoomResponse(ChatRoom room){
        this.id = room.getId();
        this.name = room.getName();
        this.type = room.getType().name();
        this.inviteCode = room.getInviteCode();
        this.createdAt = room.getCreatedAt();
    }
    
}
