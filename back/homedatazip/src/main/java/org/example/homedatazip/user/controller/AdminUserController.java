package org.example.homedatazip.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.user.dto.UserSearchRequest;
import org.example.homedatazip.user.dto.UserSearchResponse;
import org.example.homedatazip.user.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/search")
    public ResponseEntity<Page<UserSearchResponse>> searchUsers(@RequestBody @Valid UserSearchRequest userSearchRequest,
                                                                @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("관리자가 유저 찾기 - type={}, keyword={}", userSearchRequest.type(), userSearchRequest.keyword());

        Page<UserSearchResponse> userSearchResponses =
                adminUserService.searchUsers(userSearchRequest, pageable);

        log.info("관리자가 유저 찾기 완료 - userCount={}", userSearchResponses.getTotalElements());
        return ResponseEntity.ok(userSearchResponses);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {

        log.info("관리자가 유저 삭제 - userId={}", userId);

        adminUserService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
