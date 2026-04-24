package com.securechat.secure_chat.chat.controller;

import com.securechat.secure_chat.chat.dto.ChatRoomResponse;
import com.securechat.secure_chat.chat.dto.CreateRoomRequest;
import com.securechat.secure_chat.chat.dto.MessageResponse;
import com.securechat.secure_chat.chat.service.ChatRoomService;
import com.securechat.secure_chat.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            Authentication authentication,
            @Valid @RequestBody CreateRoomRequest request) {

        var room = chatRoomService.createRoom(authentication.getName(), request);
        return ResponseEntity.ok(new ChatRoomResponse(room));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(
            Authentication authentication,
            @PathVariable UUID roomId) {

        chatRoomService.joinRoom(roomId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            Authentication authentication) {

        var rooms = chatRoomService.getMyRooms(authentication.getName())
                .stream()
                .map(ChatRoomResponse::new)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        return ResponseEntity.ok(
                messageService.getMessages(roomId, authentication.getName(), page, size));
    }

    @PostMapping("/join/code")
    public ResponseEntity<ChatRoomResponse> joinByInviteCode(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String inviteCode = body.get("inviteCode");
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalArgumentException("초대 코드는 필수입니다");
        }

        var room = chatRoomService.joinByInviteCode(
                inviteCode, authentication.getName());
        return ResponseEntity.ok(new ChatRoomResponse(room));
    }

    @GetMapping("/{roomId}/invite-link")
    public ResponseEntity<Map<String, String>> getInviteLink(
            Authentication authentication,
            @PathVariable UUID roomId) {

        String link = chatRoomService.generateInviteLink(
                roomId, authentication.getName());
        return ResponseEntity.ok(Map.of(
                "inviteLink", link,
                "inviteCode", link.substring(link.lastIndexOf('/') + 1)
        ));
    }
    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            Authentication authentication,
            @PathVariable UUID roomId) {

        chatRoomService.leaveRoom(roomId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
    
}