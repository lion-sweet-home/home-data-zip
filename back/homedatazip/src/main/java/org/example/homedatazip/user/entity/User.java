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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(nullable = false)
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

    public static User create(String email, String nickname, String password, Role role) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.password = password;
        UserRole userRole = UserRole.create(user, role);
        user.roles.add(userRole);

        return user;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCustomerKey() {
        return "CUSTOMER_" + this.id;
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


}
