package com.securechat.secure_chat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String PREFIX       = "login:attempt:";
    private static final String LOCK_PREFIX  = "login:lock:";
    private static final int    MAX_ATTEMPTS = 5;          // 최대 실패 횟수
    private static final int    LOCK_MINUTES = 30;         // 잠금 시간 (분)
    private static final int    ATTEMPT_TTL  = 10;         // 실패 횟수 초기화 시간 (분)

    private final StringRedisTemplate redis;

    /** 로그인 실패 처리 */
    public void loginFailed(String username) {
        String attemptKey = PREFIX + username;

        // 실패 횟수 증가
        Long attempts = redis.opsForValue().increment(attemptKey);
        redis.expire(attemptKey, Duration.ofMinutes(ATTEMPT_TTL));

        log.warn("로그인 실패 — username: {}, 시도: {}회", username, attempts);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            // 계정 잠금
            redis.opsForValue().set(
                    LOCK_PREFIX + username,
                    "locked",
                    Duration.ofMinutes(LOCK_MINUTES)
            );
            log.warn("계정 잠금 — username: {}, {}분간 잠금", username, LOCK_MINUTES);
        }
    }

    /** 로그인 성공 처리 — 실패 횟수 초기화 */
    public void loginSucceeded(String username) {
        redis.delete(PREFIX + username);
        log.debug("로그인 성공 — 실패 횟수 초기화: {}", username);
    }

    /** 계정 잠금 여부 확인 */
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + username));
    }

    /** 남은 실패 횟수 반환 */
    public int getRemainingAttempts(String username) {
        String val = redis.opsForValue().get(PREFIX + username);
        int attempts = val != null ? Integer.parseInt(val) : 0;
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    /** 잠금 해제 (관리자용) */
    public void unlock(String username) {
        redis.delete(LOCK_PREFIX + username);
        redis.delete(PREFIX + username);
        log.info("계정 잠금 해제 — username: {}", username);
    }
}