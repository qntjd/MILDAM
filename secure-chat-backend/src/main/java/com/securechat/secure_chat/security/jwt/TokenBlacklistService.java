package com.securechat.secure_chat.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX_JTI  = "jwt:blacklist:jti:";
    private static final String PREFIX_USER = "jwt:blacklist:user:";

    private final StringRedisTemplate redis;

    /** 토큰 1개 무효화 (로그아웃 시) */
    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return;
        redis.opsForValue().set(PREFIX_JTI + jti, "1", ttl);
        log.info("토큰 무효화 완료 - jti: {}", jti);
    }

    /** 토큰이 블랙리스트에 있는지 확인 */
    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX_JTI + jti));
    }

    /** 사용자 전체 토큰 무효화 (비밀번호 변경 시) */
    public void revokeAll(String username) {
        redis.opsForValue().set(
                PREFIX_USER + username,
                Instant.now().toString(),
                Duration.ofDays(7)
        );
        log.info("사용자 전체 토큰 무효화 - username: {}", username);
    }

    /** 사용자 전체 무효화 시각 조회 */
    public Instant getUserRevokeTime(String username) {
        String val = redis.opsForValue().get(PREFIX_USER + username);
        return val != null ? Instant.parse(val) : Instant.EPOCH;
    }
}