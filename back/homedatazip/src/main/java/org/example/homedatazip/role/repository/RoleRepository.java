package org.example.homedatazip.role.repository;

import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleType(RoleType roleType);
}
