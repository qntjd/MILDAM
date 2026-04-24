package com.securechat.secure_chat.domain.message;

import com.securechat.secure_chat.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name ="message_reads")
@Getter
@NoArgsConstructor(access =AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MessageRead {

    @EmbeddedId
    private MessageReadId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @PrePersist
    protected void onCreate(){
        readAt = Instant.now();
    }
    
}
