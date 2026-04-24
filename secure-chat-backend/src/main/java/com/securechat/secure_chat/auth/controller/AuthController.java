package com.securechat.secure_chat.auth.controller;

import com.securechat.secure_chat.auth.dto.LoginRequest;
import com.securechat.secure_chat.auth.dto.RefreshTokenRequest;
import com.securechat.secure_chat.auth.dto.SignUpRequest;
import com.securechat.secure_chat.auth.dto.TokenResponse;
import com.securechat.secure_chat.auth.service.AuthService;
import com.securechat.secure_chat.security.jwt.JwtProvider;
import com.securechat.secure_chat.security.jwt.TokenBlacklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService blacklistService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String bearerToken,
            Authentication authentication) {

        String token    = bearerToken.substring(7);
        String jti      = jwtProvider.getJti(token);
        Instant expiresAt = jwtProvider.getExpiration(token).toInstant();

        blacklistService.revoke(jti, expiresAt);
        authService.logout(jti, expiresAt, authentication.getName());

        return ResponseEntity.ok("로그아웃 되었습니다");
    }
}