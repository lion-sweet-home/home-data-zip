package org.example.homedatazip.role.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.role.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    boolean existsByUser_IdAndRole_RoleType(Long userId, RoleType roleType);
    void deleteByUser_IdAndRole_RoleType(Long userId, RoleType roleType);
}
