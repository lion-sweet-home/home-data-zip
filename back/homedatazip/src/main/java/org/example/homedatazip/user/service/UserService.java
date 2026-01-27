package org.example.homedatazip.user.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.email.entity.EmailAuth;
import org.example.homedatazip.email.repository.EmailAuthRedisRepository;
import org.example.homedatazip.email.service.EmailAuthService;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.role.type.RoleType;
import org.example.homedatazip.user.dto.RegisterRequest;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailAuthRedisRepository emailAuthRepository;
    private final EmailAuthService emailAuthService;

    // 회원가입
    @Transactional
    public void register(RegisterRequest request) {

        EmailAuth auth = emailAuthService.verifyCode(request.email(), request.authCode());

        // 중복 확인
        validateDuplicateUser(request);

        // Role 확인
        Role defaultRole = roleRepository.findByRoleType(RoleType.USER)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ROLE_NOT_FOUND));

        User user = User.create(
                request.email(),
                request.nickname(),
                passwordEncoder.encode(request.password()),
                defaultRole);

        userRepository.save(user);

        // 가입 성공 후 Redis 데이터 삭제
        emailAuthRepository.delete(auth);

    }

    public boolean isNicknameAvailable(String nickname) {
        // 닉네임 중복 확인
        return userRepository.existsByNickname(nickname);
    }


    private void validateDuplicateUser(RegisterRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 확인
        if (isNicknameAvailable(request.nickname())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }


}
