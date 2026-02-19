package org.example.homedatazip.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.subscription.entity.Subscription;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.UserRole;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"login_type", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    @Column(name = "provider_id")
    private String providerId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column
    private String password;

    @Setter
    @Column(nullable = false)
    private boolean notificationEnabled = true; // 알림 수신 설정

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.PERSIST,
            orphanRemoval = true
    )
    private List<UserRole> roles = new ArrayList<>();

    @OneToOne(mappedBy = "subscriber")
    private Subscription subscription;

    public boolean hasRole(RoleType roleType) {
        return roles.stream()
                .anyMatch(role ->
                        role.getRole().getRoleType().equals(roleType));
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCustomerKey() {
        return "CUSTOMER_" + this.id;
    }

    // 전화번호 컬럼 추가
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    // 전화번호 인증 여부 체크
    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    /** 이메일/비밀번호 가입용 */
    public static User create(String email, String nickname, String password, Role role) {
        User user = new User();
        user.loginType = LoginType.LOCAL;
        user.email = email;
        user.nickname = nickname;
        user.password = password;
        UserRole userRole = UserRole.create(user, role);
        user.roles.add(userRole);

        return user;
    }

    /** 소셜 로그인 가입용 (비밀번호 없음) */
    public static User createOAuth(LoginType loginType, String providerId, String email, String nickname, Role role) {
        User user = new User();
        user.loginType = loginType;
        user.providerId = providerId;
        user.email = email;
        user.nickname = nickname;
        UserRole userRole = UserRole.create(user, role);
        user.roles.add(userRole);

        return user;
    }

    // 비밀번호 변경
    public void updatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 빈 값일 수 없습니다.");
        }
        this.password = encodedPassword;
    }

    // 구독을 위한 메서드 추가
    public void addRole(Role role) {
        if (role == null) return;

        boolean exists = this.roles.stream()
                .anyMatch(ur -> ur.getRole().getRoleType() == role.getRoleType());

        if (exists) return;

        this.roles.add(UserRole.create(this, role));
    }

    public void removeRole(RoleType roleType) {
        if (roleType == null) return;

        this.roles.removeIf(ur -> ur.getRole().getRoleType() == roleType);
    }

    //전화번호 인증관련 메서드

    //인증여부
    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    //구독 시작 전에 체크하는 용도
    public void verifyPhoneNow() {
        this.phoneVerifiedAt = LocalDateTime.now();
    }

    //인증 성공했을 때 현재 시각으로 인증 완료 처리
    public void clearPhoneVerification() {
        this.phoneVerifiedAt = null;
    }
}
