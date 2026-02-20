package org.example.homedatazip.user.repository;

import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.user.entity.LoginType;
import org.example.homedatazip.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


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

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles ur LEFT JOIN FETCH ur.role WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles ur LEFT JOIN FETCH ur.role WHERE u.id = :userId")
    Optional<User> findByIdWithRoles(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);

    // 닉네임, 이메일 포함 검색
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

    /** 알림 수신 설정한 사용자 조회 */
    List<User> findByNotificationEnabledTrue();

    /** 소셜 로그인: (loginType, providerId)로 사용자 조회 (OAuth Success Handler에서 사용) */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles ur LEFT JOIN FETCH ur.role WHERE u.loginType = :loginType AND u.providerId = :providerId")
    Optional<User> findByLoginTypeAndProviderIdWithRoles(@Param("loginType") LoginType loginType, @Param("providerId") String providerId);

}
