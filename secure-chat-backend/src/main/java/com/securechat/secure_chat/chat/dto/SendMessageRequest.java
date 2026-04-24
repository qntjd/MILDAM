package com.securechat.secure_chat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SendMessageRequest {

    @NotNull(message = "채팅방 ID는 필수입니다")
    private UUID roomId;

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 4096, message = "메시지는 4096자 이하여야 합니다")
    private String content;

    @Positive(message = "TTL은 양수여야 합니다")
    private Integer ttlSeconds;
}