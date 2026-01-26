package org.example.homedatazip.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.common.BaseTimeEntity;

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



}
