package org.example.homedatazip.global.validation;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.common.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleValidation {

    private final UserRepository userRepository;

    public void validateAdmin(Long userId) {
        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole(RoleType.ADMIN)) {
            throw new BusinessException(UserErrorCode.ADMIN_ONLY);
        }
    }
}
