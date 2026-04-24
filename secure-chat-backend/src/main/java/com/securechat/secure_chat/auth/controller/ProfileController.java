package com.securechat.secure_chat.auth.controller;

import com.securechat.secure_chat.auth.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 내 프로필 조회 */
    @GetMapping
    public ResponseEntity<ProfileService.ProfileResponse> getMyProfile(
            Authentication authentication) {
        return ResponseEntity.ok(
                profileService.getProfile(authentication.getName()));
    }

    /** 다른 사용자 프로필 조회 */
    @GetMapping("/{username}")
    public ResponseEntity<ProfileService.ProfileResponse> getProfile(
            @PathVariable String username) {
        return ResponseEntity.ok(profileService.getProfile(username));
    }

    /** 닉네임 + 상태 메시지 수정 */
    @PutMapping
    public ResponseEntity<ProfileService.ProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String nickname      = body.get("nickname");
        String statusMessage = body.get("statusMessage");

        return ResponseEntity.ok(
                profileService.updateProfile(
                        authentication.getName(), nickname, statusMessage));
    }

    /** 프로필 이미지 업로드 */
    @PostMapping("/image")
    public ResponseEntity<ProfileService.ProfileResponse> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(
                profileService.updateProfileImage(authentication.getName(), file));
    }

    /** 프로필 이미지 파일 제공 */
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getProfileImage(
            @PathVariable String filename) throws MalformedURLException {

        Path path = Paths.get("uploads/profiles/").resolve(filename);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}