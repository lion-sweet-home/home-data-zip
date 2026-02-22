package org.example.homedatazip.role;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.user.entity.User;

@Entity
@Getter
@Table(
        name = "user_role",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "role_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public static UserRole create(User user, Role role) {
        if (user == null) {
            throw new IllegalArgumentException("UserRole.create: user가 null 입니다.");
        }
        if (role == null) {
            throw new IllegalArgumentException("UserRole.create: role이 null 입니다.");
        }
        if (role.getId() == null) {
            throw new IllegalArgumentException("UserRole.create: role.id가 null 입니다.");
        }

        UserRole userRole = new UserRole();
        userRole.user = user;
        userRole.role = role;
        return userRole;
    }
}