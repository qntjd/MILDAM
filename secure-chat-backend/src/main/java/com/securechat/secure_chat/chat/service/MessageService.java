package com.securechat.secure_chat.chat.service;

import com.securechat.secure_chat.chat.dto.MessageResponse;
import com.securechat.secure_chat.chat.dto.SendMessageRequest;
import com.securechat.secure_chat.domain.message.Message;
import com.securechat.secure_chat.domain.message.MessageRepository;
import com.securechat.secure_chat.domain.user.User;
import com.securechat.secure_chat.domain.user.UserRepository;
import com.securechat.secure_chat.domain.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate broker;

    @Transactional
    public MessageResponse sendMessage(String username, SendMessageRequest request) {
        ChatRoom room = chatRoomService.validateMembership(
                request.getRoomId(), username);

        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Instant expiresAt = request.getTtlSeconds() != null
                ? Instant.now().plusSeconds(request.getTtlSeconds())
                : null;

        Message message = Message.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .iv("")
                .expiresAt(expiresAt)
                .build();

        Message saved = messageRepository.save(message);

        MessageResponse response = new MessageResponse(saved, request.getContent());

        // WebSocket으로 채팅방 구독자 전체에게 전송
        broker.convertAndSend("/topic/chat." + request.getRoomId(), response);

        return response;
    }

    @Transactional
    public void deleteMessage(UUID messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다"));

        if (!message.getSender().getUsername().equals(username)) {
            throw new SecurityException("본인 메시지만 삭제할 수 있습니다");
        }

        message.markDeleted();
        broker.convertAndSend(
                "/topic/chat." + message.getRoom().getId(),
                new DeleteNotice(messageId)
        );
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID roomId, String username, int page, int size) {
        chatRoomService.validateMembership(roomId, username);

        return messageRepository
                .findByRoomIdAndDeletedFalseOrderByCreatedAtDesc(
                        roomId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(m -> new MessageResponse(m, m.getContent()))
                .toList();
    }

    // 만료 메시지 1분마다 자동 삭제
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void purgeExpiredMessages() {
        int count = messageRepository.markExpiredAsDeleted(Instant.now());
        if (count > 0) {
            log.info("만료 메시지 {}건 삭제", count);
        }
    }

    public record DeleteNotice(UUID messageId) {
        public String getType() { return "DELETE"; }
    }
}