package org.example.homedatazip.user.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.email.entity.EmailAuth;
import org.example.homedatazip.email.repository.EmailAuthRedisRepository;
import org.example.homedatazip.email.service.EmailAuthService;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.notification.service.SseEmitterService;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.UserRole;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.role.type.RoleType;
import org.example.homedatazip.user.dto.*;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailAuthRedisRepository emailAuthRepository;
    private final SseEmitterService sseEmitterService;
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

    @Transactional
    public void updateNotificationSetting(Long userId, NotificationSettingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        boolean previousSetting = user.isNotificationEnabled();
        user.setNotificationEnabled(request.notificationEnabled());

        // 알림 수신 설정을 false로 변경한 경우 SSE 연결 종료
        if (previousSetting && !request.notificationEnabled()) {
            sseEmitterService.removeEmitter(userId);
        }
    }

    //FIXME: 관리자가 임의로 사용자의 정보를 수정할 수 있다면 관리자 검증로직이 추가되어야함
    @Transactional(readOnly = true)
    public MyPageResponse getMyPageInfo(Long userId, String email){
        validUser(email, userId);

        User targetUser = findUserById(userId);

        return MyPageResponse.from(targetUser);
    }

    @Transactional
    public MyPageResponse editMyPage(MyPageEditRequest request, String email, Long userId){

        //수정 마이페이지 주인인 타켓유저(userId)와 실제 요청한 유저(email) 검증
        validUser(email, userId);

        //수정 페이지의 주인 값을 바꿔야함으로 userId로 찾은 user의 정보를 수정
        User targetUser = findUserById(userId);

        if (!passwordEncoder.matches(request.password(), targetUser.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_PASSWORD);
        }

        //수정하려는 닉네임의 존재여부 확인은 프론트에서 확인으로 따로 api호출함으로 생략

        //닉네임수정
        targetUser.changeNickname(request.nickname());

        return MyPageResponse.from(targetUser);
    }


    private User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private User findUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validUser(String email, Long userId) {
        User user = findUserByEmail(email);

        if(!user.getId().equals(userId))
            throw new BusinessException(UserErrorCode.ACCESS_DENIED);
    }

    private boolean isAdmin(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<String> roles = stringFromUserRole(user);

        return roles.contains("ADMIN");
    }

    private List<String> stringFromUserRole(User user){
        return user.getRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getRoleType)
                .distinct()
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}

