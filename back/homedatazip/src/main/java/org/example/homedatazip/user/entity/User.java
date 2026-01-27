package org.example.homedatazip.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.role.UserRole;
import org.example.homedatazip.subscription.entity.Subscription;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
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
                .anyMatch(role -> role.getRole().equals(roleType));
    }
}