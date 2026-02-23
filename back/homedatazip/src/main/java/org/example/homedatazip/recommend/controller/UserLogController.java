package org.example.homedatazip.recommend.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.recommend.dto.UserPyeongClickRequest;
import org.example.homedatazip.recommend.service.SearchLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class UserLogController {
    private final SearchLogService searchLogService;

    @PostMapping("/click")
    public ResponseEntity<Void> logClick(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UserPyeongClickRequest req) {
        if (user != null) {
            searchLogService.savePyeongClickLog(user.getUserId(), req);
        }
        return ResponseEntity.ok().build();
    }
}
