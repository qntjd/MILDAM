package com.securechat.secure_chat.chat.dto;

import com.securechat.secure_chat.domain.chat.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateRoomRequest {

    @NotBlank(message = "방 이름은 필수입니다")
    @Size(min = 1, max = 50, message = "방 이름은 1~50자여야 합니다")
    @Pattern(
        regexp = "^[a-zA-Z0-9가-힣\\s\\-_]+$",
        message = "방 이름에 특수문자는 사용할 수 없습니다"
    )
    private String name;

    @NotNull(message = "방 타입은 필수입니다")
    private ChatRoomType type;
}