package com.securechat.secure_chat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SecretMessageRequest {

    @NotBlank(message = "세션 ID는 필수입니다")
    private String sessionId;

    @NotBlank(message = "암호문은 필수입니다")
    private String cipherText;

    private Integer ttlSeconds;
}