package com.securechat.secure_chat.auth.service;

import com.securechat.secure_chat.domain.user.User;
import com.securechat.secure_chat.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    // 프로필 이미지 저장 경로
    private static final String UPLOAD_DIR = "uploads/profiles/";

    /** 프로필 조회 */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String username) {
        User user = findUser(username);
        return ProfileResponse.from(user);
    }

    /** 닉네임 + 상태 메시지 수정 */
    @Transactional
    public ProfileResponse updateProfile(String username, String nickname,
                                          String statusMessage) {
        User user = findUser(username);
        user.updateProfile(nickname, statusMessage);
        return ProfileResponse.from(user);
    }

    /** 프로필 이미지 업로드 */
    @Transactional
    public ProfileResponse updateProfileImage(String username, MultipartFile file)
            throws IOException {

        // 파일 검증
        validateImageFile(file);

        User user = findUser(username);

        // 기존 이미지 삭제
        if (user.getProfileImageUrl() != null) {
            deleteOldImage(user.getProfileImageUrl());
        }

        // 새 이미지 저장
        String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        Files.write(uploadPath.resolve(filename), file.getBytes());

        String imageUrl = "/api/profile/images/" + filename;
        user.updateProfileImage(imageUrl);

        log.info("프로필 이미지 업로드 - username: {}, filename: {}", username, filename);
        return ProfileResponse.from(user);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다");
        }

        // 파일 크기 제한 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 5MB 이하여야 합니다");
        }

        // 확장자 검증
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!ext.equals(".jpg") && !ext.equals(".jpeg")
                && !ext.equals(".png") && !ext.equals(".gif")) {
            throw new IllegalArgumentException("jpg, jpeg, png, gif 파일만 업로드 가능합니다");
        }

        // MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    private void deleteOldImage(String imageUrl) {
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("기존 이미지 삭제 실패: {}", e.getMessage());
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다: " + username));
    }

    // ── DTO ──────────────────────────────────────────────────────

    public record ProfileResponse(
            String username,
            String nickname,
            String displayName,
            String statusMessage,
            String profileImageUrl
    ) {
        public static ProfileResponse from(User user) {
            return new ProfileResponse(
                    user.getUsername(),
                    user.getNickname(),
                    user.getDisplayName(),
                    user.getStatusMessage(),
                    user.getProfileImageUrl()
            );
        }
    }
}