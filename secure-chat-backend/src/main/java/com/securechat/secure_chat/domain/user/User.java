package com.securechat.secure_chat.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Builder;


@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length= 50)
    private String username;

    @Column (nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length=50)
    private String nickname;

    @Column(name="status_message", length=200)
    private String statusMessage;

    @Column(name="profile_image_url", length=500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void changePassword(String encodedPassword){
        this.password = encodedPassword;
    }

    public void updateProfile(String nickname, String statusMessage){
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (statusMessage != null){
            this.statusMessage = statusMessage;
        }
    }

    public void updateProfileImage(String profileImageUrl){
        this.profileImageUrl = profileImageUrl;
    }

    public String getDisplayName() {
        return nickname != null ? nickname : username;
    }

}
