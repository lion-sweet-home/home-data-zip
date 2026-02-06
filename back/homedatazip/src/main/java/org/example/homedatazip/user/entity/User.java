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
}
