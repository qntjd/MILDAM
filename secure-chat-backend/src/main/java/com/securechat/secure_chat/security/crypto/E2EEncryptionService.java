package com.securechat.secure_chat.security.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * X25519 ECDH 키 교환 + AES-256-GCM 메시지 암호화
 *
 * 핵심 원리:
 *   1. 양쪽 클라이언트가 X25519 키 쌍 생성
 *   2. 공개키만 서버를 통해 교환 (비밀키는 절대 서버로 전송 안 함)
 *   3. 각자 상대방 공개키 + 내 비밀키로 동일한 공유 비밀키 도출
 *   4. 공유 비밀키로 AES-256-GCM 암호화
 *   5. 서버는 암호문만 중계 — 내용 해독 불가
 */
@Slf4j
@Service
public class E2EEncryptionService {

    private static final int GCM_IV_LEN  = 12;
    private static final int GCM_TAG_LEN = 128;
    private final SecureRandom rng = new SecureRandom();

    // ── 키 쌍 생성 ────────────────────────────────────────────────

    public KeyPairDto generateKeyPair() {
        var gen = new X25519KeyPairGenerator();
        gen.init(new X25519KeyGenerationParameters(rng));
        var kp = gen.generateKeyPair();

        byte[] pub  = ((X25519PublicKeyParameters)  kp.getPublic()).getEncoded();
        byte[] priv = ((X25519PrivateKeyParameters) kp.getPrivate()).getEncoded();

        return new KeyPairDto(
            Base64.getEncoder().encodeToString(pub),
            Base64.getEncoder().encodeToString(priv)
        );
    }

    // ── 공유 비밀키 도출 (ECDH) ───────────────────────────────────

    /**
     * Alice: derive(alice_priv, bob_pub)
     * Bob:   derive(bob_priv,   alice_pub)
     * 수학적으로 동일한 값 도출 — 서버는 절대 알 수 없음
     */
    public byte[] deriveSharedSecret(String myPrivB64, String theirPubB64) {
        try {
            var myPriv   = new X25519PrivateKeyParameters(
                    Base64.getDecoder().decode(myPrivB64), 0);
            var theirPub = new X25519PublicKeyParameters(
                    Base64.getDecoder().decode(theirPubB64), 0);

            var agreement = new X25519Agreement();
            agreement.init(myPriv);

            byte[] raw = new byte[32];
            agreement.calculateAgreement(theirPub, raw, 0);

            return hkdfExpand(raw);
        } catch (Exception e) {
            throw new CryptoException("공유 비밀키 도출 실패", e);
        }
    }

    /** HKDF-SHA256 키 유도 — 원시 DH 출력을 직접 쓰지 않음 */
    private byte[] hkdfExpand(byte[] rawSecret) throws Exception {
        byte[] salt = "securechat-v1".getBytes();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(rawSecret);

        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update("AES-256-GCM".getBytes());
        mac.update((byte) 1);
        return mac.doFinal();
    }

    // ── Key Fingerprint (MITM 탐지) ───────────────────────────────

    /**
     * 공유 비밀키의 SHA-256 앞 20바이트 → hex 문자열
     * 양쪽 클라이언트가 이 값을 비교해서 중간자 공격 탐지
     */
    public String computeFingerprint(byte[] sharedSecret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sharedSecret);
            return HexFormat.of().formatHex(Arrays.copyOf(hash, 20));
        } catch (Exception e) {
            throw new CryptoException("Fingerprint 생성 실패", e);
        }
    }

    /** Fingerprint → 이모지 5개 (사용자가 시각적으로 비교) */
    public String toEmojiFingerprint(String hexFp) {
        String[] pool = {
            "🌊","🌸","🍎","🌺","🦁","🐬","🦋","🌙","⭐","🔥",
            "🍀","🌈","🎵","💎","🌻","🦚","🍁","🎭","🧊","🌾",
            "🏔️","🦅","🐾","🎪","🌠","🍄","🌲","🦈","🎯","🦜"
        };
        var sb = new StringBuilder();
        for (int i = 0; i < 10; i += 2) {
            int idx = Integer.parseInt(hexFp.substring(i, i + 2), 16) % pool.length;
            sb.append(pool[idx]);
        }
        return sb.toString();
    }

    // ── 메시지 암호화 / 복호화 ────────────────────────────────────

    /**
     * AES-256-GCM 암호화
     * 반환: Base64(IV[12] + CipherText + GCM-Tag[16])
     * messageId를 AAD로 사용 → 재전송 공격 방지
     */
    public String encrypt(String plainText, byte[] sharedSecret, String messageId) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            rng.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(sharedSecret, "AES"),
                    new GCMParameterSpec(GCM_TAG_LEN, iv));
            cipher.updateAAD(messageId.getBytes());

            byte[] ct = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);

        } catch (Exception e) {
            throw new CryptoException("암호화 실패", e);
        }
    }

    public String decrypt(String encB64, byte[] sharedSecret, String messageId) {
        try {
            byte[] data = Base64.getDecoder().decode(encB64);
            byte[] iv   = Arrays.copyOf(data, GCM_IV_LEN);
            byte[] ct   = Arrays.copyOfRange(data, GCM_IV_LEN, data.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(sharedSecret, "AES"),
                    new GCMParameterSpec(GCM_TAG_LEN, iv));
            cipher.updateAAD(messageId.getBytes());

            return new String(cipher.doFinal(ct), "UTF-8");

        } catch (javax.crypto.AEADBadTagException e) {
            log.error("GCM 태그 불일치 — 메시지 변조 의심");
            throw new MessageTamperedException("메시지가 변조되었습니다");
        } catch (Exception e) {
            throw new CryptoException("복호화 실패", e);
        }
    }

    // ── Records & Exceptions ──────────────────────────────────────

    public record KeyPairDto(String publicKey, String privateKey) {}

    public static class CryptoException extends RuntimeException {
        public CryptoException(String m, Throwable c) { super(m, c); }
    }

    public static class MessageTamperedException extends RuntimeException {
        public MessageTamperedException(String m) { super(m); }
    }
}