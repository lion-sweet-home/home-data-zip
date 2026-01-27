package org.example.homedatazip.user.repository;

import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


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
}