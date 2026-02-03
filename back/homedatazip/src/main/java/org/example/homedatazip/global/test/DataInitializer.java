package org.example.homedatazip.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initAdmin();
        initTestUser();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(RoleType.USER));
            roleRepository.save(new Role(RoleType.SELLER));
            roleRepository.save(new Role(RoleType.ADMIN));
            log.info("✅ Role 초기 데이터 생성 완료");
        }
    }

    private void initAdmin() {
        String adminEmail = "admin@example.com";
        String adminPassword = "Admin1234!@";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByRoleType(RoleType.ADMIN)
                    .orElseThrow(() -> new BusinessException(UserErrorCode.ROLE_NOT_FOUND));

            String encodedPassword = passwordEncoder.encode(adminPassword);
            log.info("🔐 생성된 해시: {}", encodedPassword);  // 해시값 확인용

            User admin = User.create(
                    adminEmail,
                    "Admin",
                    encodedPassword,
                    adminRole
            );

            userRepository.save(admin);
            log.info("✅ Admin 유저 생성: {} / {}", adminEmail, adminPassword);
        } else {
            log.info("ℹ️ Admin 유저 이미 존재: {}", adminEmail);
        }
    }

    private void initTestUser() {
        String testEmail = "test@example.com";
        String testPassword = "Test1234!@";

        if (userRepository.findByEmail(testEmail).isEmpty()) {
            Role userRole = roleRepository.findByRoleType(RoleType.USER)
                    .orElseThrow(() -> new BusinessException(UserErrorCode.ROLE_NOT_FOUND));

            String encodedPassword = passwordEncoder.encode(testPassword);
            log.info("🔐 생성된 해시: {}", encodedPassword);  // 해시값 확인용

            User testUser = User.create(
                    testEmail,
                    "테스트유저",
                    encodedPassword,
                    userRole
            );

            userRepository.save(testUser);
            log.info("✅ 테스트 유저 생성: {} / {}", testEmail, testPassword);
        } else {
            log.info("ℹ️ 테스트 유저 이미 존재: {}", testEmail);
        }
    }
}