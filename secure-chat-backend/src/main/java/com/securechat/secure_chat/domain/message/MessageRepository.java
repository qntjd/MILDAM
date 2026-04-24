package com.securechat.secure_chat.domain.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Slice<Message> findByRoomIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID roomId, Pageable pageable);

    List<Message> findByDeletedFalseAndExpiresAtBefore(Instant now);

    @Modifying
    @Query("UPDATE Message m SET m.deleted = true, m.content = '' " +
           "WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now AND m.deleted = false")
    int markExpiredAsDeleted(@Param("now") Instant now);
}