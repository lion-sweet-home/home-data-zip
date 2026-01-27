package org.example.homedatazip.role;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private RoleType roleType;
}