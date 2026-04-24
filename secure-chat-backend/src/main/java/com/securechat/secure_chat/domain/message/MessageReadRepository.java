package com.securechat.secure_chat.domain.message;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId>{

    @Query("SELECT COUNT(r) FROM MessageRead r WHERE r.message.id = :messageId")
    long countByMessageId(@Param("messageId") UUID messageId);

    @Query("SELECT r.id.messageId FROM MessageRead r WHERE r.id.userId = :userId AND r.message.room.id = :roomId")
    List<UUID> findReadMessageIdsByUserIdAndRoomId(

            @Param("userId") UUID userId,
            @Param("roomId") UUID roomId);

    boolean existsById(MessageReadId id);

    @Query("SELECT r.user.username FROM MessageRead r WHERE r.message.id = :messageId")
    List<String> findByReadersByMessageId(@Param("messageId") UUID messageId);
    
}
