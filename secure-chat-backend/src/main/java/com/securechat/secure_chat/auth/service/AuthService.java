package com.securechat.secure_chat.auth.service;

import com.securechat.secure_chat.auth.dto.LoginRequest;
import com.securechat.secure_chat.auth.dto.SignUpRequest;
import com.securechat.secure_chat.auth.dto.TokenResponse;
import com.securechat.secure_chat.domain.user.User;
import com.securechat.secure_chat.domain.user.UserRepository;
import com.securechat.secure_chat.security.LoginAttemptService;
import com.securechat.secure_chat.security.jwt.JwtProperties;
import com.securechat.secure_chat.security.jwt.JwtProvider;
import com.securechat.secure_chat.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        log.info("회원가입 완료 - username: {}", request.getUsername());
    }

    public TokenResponse login(LoginRequest request) {
    // 계정 잠금 확인
    if (loginAttemptService.isLocked(request.getUsername())) {
        throw new IllegalStateException(
            "계정이 잠겼습니다. 30분 후 다시 시도해주세요");
    }

    try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
    } catch (Exception e) {
        // 로그인 실패 처리
        loginAttemptService.loginFailed(request.getUsername());
        int remaining = loginAttemptService.getRemainingAttempts(request.getUsername());
        throw new IllegalArgumentException(
            "아이디 또는 비밀번호가 올바르지 않습니다. 남은 시도: " + remaining + "회"
        );
    }

    // 로그인 성공
    loginAttemptService.loginSucceeded(request.getUsername());

    User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    String accessToken  = jwtProvider.generateAccessToken(
            user.getUsername(), user.getRole().name());
    String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());

    refreshTokenService.save(
            user.getUsername(),
            refreshToken,
            jwtProperties.getRefreshTokenExpiry()
    );

    return new TokenResponse(accessToken, refreshToken);
}

    public TokenResponse refresh(String refreshToken) {
        // Refresh Token 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        String username = jwtProvider.getUsername(refreshToken);

        // Redis에 저장된 토큰과 비교
        if (!refreshTokenService.isValid(username, refreshToken)) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 Refresh Token입니다");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 새 토큰 발급
        String newAccessToken  = jwtProvider.generateAccessToken(
                user.getUsername(), user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getUsername());

        // Refresh Token Rotation — 기존 토큰 삭제 후 새 토큰 저장
        refreshTokenService.save(
                user.getUsername(),
                newRefreshToken,
                jwtProperties.getRefreshTokenExpiry()
        );

        log.info("토큰 재발급 완료 - username: {}", username);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String jti, java.time.Instant expiresAt, String username) {
        refreshTokenService.delete(username);
    }
}