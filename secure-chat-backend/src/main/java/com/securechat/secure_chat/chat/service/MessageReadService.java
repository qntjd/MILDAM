package com.securechat.secure_chat.chat.service;

import com.securechat.secure_chat.domain.message.Message;
import com.securechat.secure_chat.domain.message.MessageRead;
import com.securechat.secure_chat.domain.message.MessageReadId;
import com.securechat.secure_chat.domain.message.MessageReadRepository;
import com.securechat.secure_chat.domain.message.MessageRepository;
import com.securechat.secure_chat.domain.user.User;
import com.securechat.secure_chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReadService {

    private final MessageReadRepository messageReadRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate broker;

    @Transactional
    public void markAsRead(UUID messageId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다"));

        MessageReadId id = new MessageReadId(messageId, user.getId());

        if (messageReadRepository.existsById(id)) return;

        messageReadRepository.save(MessageRead.builder()
                .id(id)
                .message(message)
                .user(user)
                .build());

        long readCount = messageReadRepository.countByMessageId(messageId);
        List<String> readers = messageReadRepository.findByReadersByMessageId(messageId);

        broker.convertAndSend(
                "/topic/chat." + message.getRoom().getId() + ".read",
                new ReadNotice(messageId, username, readCount,readers)
        );

        log.debug("읽음 처리 - messageId: {}, username: {}, readCount: {}",
                messageId, username, readCount);
    }

    @Transactional
    public void markAllAsRead(UUID roomId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        List<UUID> alreadyReadIds = messageReadRepository
                .findReadMessageIdsByUserIdAndRoomId(user.getId(), roomId);

        messageRepository
                .findByRoomIdAndDeletedFalseOrderByCreatedAtDesc(
                        roomId, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .filter(m -> !alreadyReadIds.contains(m.getId()))
                .forEach(m -> {
                    MessageReadId id = new MessageReadId(m.getId(), user.getId());
                    messageReadRepository.save(MessageRead.builder()
                            .id(id)
                            .message(m)
                            .user(user)
                            .build());
                        long readCount = messageReadRepository.countByMessageId(m.getId());
                        List<String> readers = messageReadRepository.findByReadersByMessageId(m.getId());
                        broker.convertAndSend(
                                "/topic/chat." + roomId + ".read",
                                new ReadNotice(m.getId(), username, readCount, readers)
                        );
                });
                
    

        log.debug("일괄 읽음 처리 - roomId: {}, username: {}", roomId, username);
    }

    @Transactional
    public long getReadCount(UUID messageId) {
        return messageReadRepository.countByMessageId(messageId);
    }

    public record ReadNotice(UUID messageId, String username, long readCount, List<String> readers) {}
}