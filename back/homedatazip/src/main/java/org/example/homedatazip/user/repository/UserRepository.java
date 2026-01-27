package org.example.homedatazip.user.repository;

import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<User> findByEmail(String email);
}
