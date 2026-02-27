package com.mysite.sbb.user;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mysite.sbb.DataNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 업로드 루트 폴더 (application.properties에서 설정)
    @Value("${app.upload.dir}")
    private String uploadDir;

    public SiteUser create(String username, String email, String password) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        this.userRepository.save(user);
        return user;
    }

    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByusername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    // ✅ 추가: 프로필 이미지 업로드 처리
    public void updateProfileImage(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }

        // 1) 유저 조회
        SiteUser user = getUser(username);

        // 2) 이미지 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // 3) 확장자 결정(보수적으로 content-type 기반)
        String ext = toExtension(contentType);

        // 4) 저장 경로 생성: profiles/{userId}/{uuid}.ext
        String filename = UUID.randomUUID().toString() + ext;
        Path userDir = Paths.get(uploadDir, "profiles", String.valueOf(user.getId()));
        Path savePath = userDir.resolve(filename);

        try {
            Files.createDirectories(userDir);
            file.transferTo(savePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }

        // 5) DB에는 /uploads 아래에서 찾을 상대 경로만 저장
        String relativePath = Paths.get("profiles", String.valueOf(user.getId()), filename)
                                  .toString()
                                  .replace("\\", "/");
        user.setProfileImagePath(relativePath);
        userRepository.save(user);
    }

    private String toExtension(String contentType) {
        // 면접 포인트: 확장자 신뢰 X, 타입 기반으로 제한
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".png"; // 그 외는 안전하게 png로 저장(혹은 거절해도 됨)
        };
    }
}
