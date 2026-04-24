package com.securechat.secure_chat.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redis;

    /** Refresh Token 저장 */
    public void save(String username, String refreshToken, long expirySeconds) {
        redis.opsForValue().set(
                PREFIX + username,
                refreshToken,
                Duration.ofSeconds(expirySeconds)
        );
        log.debug("Refresh Token 저장 - username: {}", username);
    }

    /** Refresh Token 검증 */
    public boolean isValid(String username, String refreshToken) {
        String stored = redis.opsForValue().get(PREFIX + username);
        return refreshToken.equals(stored);
    }

    /** Refresh Token 삭제 (로그아웃 시) */
    public void delete(String username) {
        redis.delete(PREFIX + username);
        log.debug("Refresh Token 삭제 - username: {}", username);
    }
}