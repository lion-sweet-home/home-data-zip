package org.example.homedatazip.user.repository;

import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select count(distinct u) from User u
            join u.roles ur
            join ur.role r
            where r.roleType = :roleType
            """)
    Long countByRoleType(RoleType roleType);

    @Query("""
            select count(distinct u) from User u
            where u.id not in (
            select ur.user.id from UserRole ur
            join ur.role r
            where r.roleType = :roleType
            )
            """)
    Long countByRoleTypeNot(@Param("roleType") RoleType roleType);

    @Query("""
            select distinct u from User u
            where u.id not in (
            select ur.user.id from UserRole ur
            join ur.role r
            where r.roleType = :roleType
            )
            """)
    Page<User> findByRoleTypeNot(
            @Param("roleType") RoleType roleType,
            Pageable pageable
    );

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 알림 수신 설정한 사용자 조회
    List<User> findByNotificationEnabledTrue();
    // 닉네임, 이메일 포함해서 검색
    @Query("""
        select u from User u
        where u.nickname like %:keyword% or u.email like %:keyword%
""")
    Page<User> searchAll(@Param("keyword") String keyword, Pageable pageable);

    // 닉네임 검색
    @Query("""
        select u from User u
        where u.nickname like %:keyword%
""")
    Page<User> searchByNickname(@Param("keyword") String keyword, Pageable pageable);

    // 이메일 검색
    @Query("""
        select u from User u
        where u.email like %:keyword%
""")
    Page<User> searchByEmail(@Param("keyword") String keyword, Pageable pageable);
}
