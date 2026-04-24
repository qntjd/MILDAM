package com.securechat.secure_chat.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시크릿 채팅 세션 관리
 *
 * 서버 역할 — 딱 3가지만:
 *   1. 공개키 중계
 *   2. 암호문 중계 (서버는 내용 해독 불가)
 *   3. 세션 메타데이터 관리
 *
 * 비밀키, 공유 비밀키는 절대 서버에 없음 → Zero Trust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecretChatService {

    private final SimpMessagingTemplate broker;
    private final StringRedisTemplate redis;

    // 활성 세션 (메모리 — DB 저장 안 함)
    private final ConcurrentHashMap<String, SecretSession> sessions
            = new ConcurrentHashMap<>();

    // ── 세션 초기화 ────────────────────────────────────────────────

    public String initSession(String initiatorId, String recipientId) {
        String sessionId = UUID.randomUUID().toString();

        sessions.put(sessionId, new SecretSession(
                sessionId, initiatorId, recipientId,
                SessionPhase.WAITING, Instant.now(), null, null));

        // 상대방에게 시크릿 채팅 요청 알림
        broker.convertAndSendToUser(recipientId, "/queue/secret-request",
                Map.of("sessionId", sessionId, "from", initiatorId));

        log.info("시크릿 세션 초기화 — {} → {}, sessionId: {}",
                initiatorId, recipientId, sessionId);
        return sessionId;
    }

    // ── 공개키 중계 ────────────────────────────────────────────────

    public void relayPublicKey(String sessionId, String senderId,
                                String publicKeyBase64) {
        SecretSession session = getSession(sessionId);
        String recipientId = getOtherUser(session, senderId);

        // 서버는 공개키를 전달만 하고 내용을 알 수 없음
        broker.convertAndSendToUser(recipientId, "/queue/secret-key",
                Map.of("sessionId", sessionId,
                       "from", senderId,
                       "publicKey", publicKeyBase64));

        log.debug("공개키 중계 — sessionId: {}, from: {}", sessionId, senderId);
    }

    // ── Key Fingerprint 확인 ───────────────────────────────────────

    public void confirmKeyExchange(String sessionId, String userId,
                                    String emojiFingerprint) {
        SecretSession session = getSession(sessionId);

        if (userId.equals(session.initiatorId())) {
            sessions.put(sessionId, session.withInitiatorFp(emojiFingerprint));
        } else {
            sessions.put(sessionId, session.withRecipientFp(emojiFingerprint));
        }

        session = getSession(sessionId);

        if (session.initiatorFp() != null && session.recipientFp() != null) {
            if (session.initiatorFp().equals(session.recipientFp())) {
                // 지문 일치 → 안전한 채널
                sessions.put(sessionId, session.withPhase(SessionPhase.ACTIVE));
                log.info("시크릿 세션 활성화 — sessionId: {}", sessionId);
                notifyBoth(session, "/queue/secret-ready",
                        Map.of("sessionId", sessionId, "status", "ACTIVE"));
            } else {
                // 지문 불일치 → MITM 공격 가능성!
                log.error("키 지문 불일치! MITM 공격 의심 — sessionId: {}", sessionId);
                terminateSession(sessionId, "KEY_FINGERPRINT_MISMATCH");
            }
        }
    }

    // ── 암호화 메시지 중계 ─────────────────────────────────────────

    public String relayEncryptedMessage(String sessionId, String senderId,
                                         String cipherText, Integer ttlSeconds) {
        SecretSession session = getSession(sessionId);

        if (session.phase() != SessionPhase.ACTIVE) {
            throw new IllegalStateException("활성화되지 않은 세션입니다");
        }

        String messageId   = UUID.randomUUID().toString();
        String recipientId = getOtherUser(session, senderId);

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("messageId",  messageId);
        envelope.put("sessionId",  sessionId);
        envelope.put("cipherText", cipherText); // 서버는 복호화 불가
        envelope.put("timestamp",  Instant.now().toString());

        if (ttlSeconds != null) {
            envelope.put("expiresAt",
                    Instant.now().plusSeconds(ttlSeconds).toString());
            redis.opsForValue().set(
                    "secret:msg:" + messageId,
                    sessionId,
                    Duration.ofSeconds(ttlSeconds));
        }

        broker.convertAndSendToUser(recipientId, "/queue/secret-message", envelope);
        return messageId;
    }

    // ── 메시지 양방향 삭제 ─────────────────────────────────────────

    public void deleteMessage(String sessionId, String messageId) {
        SecretSession session = getSession(sessionId);
        notifyBoth(session, "/queue/secret-delete",
                Map.of("messageId", messageId, "sessionId", sessionId));
        redis.delete("secret:msg:" + messageId);
    }

    // ── 세션 종료 (PFS 보장) ───────────────────────────────────────

    public void terminateSession(String sessionId, String reason) {
        SecretSession session = sessions.remove(sessionId);
        if (session == null) return;

        notifyBoth(session, "/queue/secret-terminated",
                Map.of("sessionId", sessionId, "reason", reason));

        log.info("시크릿 세션 종료 — sessionId: {}, reason: {}", sessionId, reason);
        // 클라이언트는 이 시점에 로컬 키 파기 → PFS 완성
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private SecretSession getSession(String sessionId) {
        SecretSession s = sessions.get(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        return s;
    }

    private String getOtherUser(SecretSession session, String userId) {
        return userId.equals(session.initiatorId())
                ? session.recipientId()
                : session.initiatorId();
    }

    private void notifyBoth(SecretSession session, String dest, Object payload) {
        broker.convertAndSendToUser(session.initiatorId(), dest, payload);
        broker.convertAndSendToUser(session.recipientId(), dest, payload);
    }

    // ── Session 레코드 ─────────────────────────────────────────────

    public enum SessionPhase {
        WAITING, KEY_EXCHANGE, ACTIVE, TERMINATED
    }

    public record SecretSession(
            String sessionId,
            String initiatorId,
            String recipientId,
            SessionPhase phase,
            Instant createdAt,
            String initiatorFp,
            String recipientFp
    ) {
        SecretSession withPhase(SessionPhase p) {
            return new SecretSession(sessionId, initiatorId, recipientId,
                    p, createdAt, initiatorFp, recipientFp);
        }
        SecretSession withInitiatorFp(String fp) {
            return new SecretSession(sessionId, initiatorId, recipientId,
                    phase, createdAt, fp, recipientFp);
        }
        SecretSession withRecipientFp(String fp) {
            return new SecretSession(sessionId, initiatorId, recipientId,
                    phase, createdAt, initiatorFp, fp);
        }
    }
}