package org.example.homedatazip.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.settlement.dto.SettlementResponse;
import org.example.homedatazip.user.dto.UserResponse;
import org.example.homedatazip.user.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * 이번 달 수입 조회
     */
    @GetMapping("/monthly-income")
    public ResponseEntity<Long> getMonthlyIncome(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // FIXME: CustomUserDetails 필드 확인 후 수정
        Long userId = userDetails.getUserId();

        Long monthlyIncome
                = adminService.getMonthlyIncome(userId);

        return ResponseEntity.ok().body(monthlyIncome);
    }

    /**
     * 월별 수입 조회
     */
    @GetMapping("/yearly-income")
    public ResponseEntity<List<SettlementResponse>> getYearlyIncome(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year
    ) {
        // FIXME: CustomUserDetails 필드 확인 후 수정
        Long userId = userDetails.getUserId();

        List<SettlementResponse> yearlyIncome
                = adminService.getYearlyIncome(userId, year);

        return ResponseEntity.ok().body(yearlyIncome);
    }

    /**
     * 총 회원 수 조회
     */
    @GetMapping("/users/count")
    public ResponseEntity<Long> getUsersCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // FIXME: CustomUserDetails 필드 확인 후 수정
        Long userId = userDetails.getUserId();

        Long userCount
                = adminService.getUserCount(userId);

        return ResponseEntity.ok().body(userCount);
    }

    /**
     * 유저 목록 조회
     */
    @GetMapping("/users/list")
    public ResponseEntity<Page<UserResponse>> getUsersList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault Pageable pageable
    ) {
        // FIXME: CustomUserDetails 필드 확인 후 수정
        Long userId = userDetails.getUserId();

        Page<UserResponse> usersList
                = adminService.getUserList(userId, pageable);

        return ResponseEntity.ok().body(usersList);
    }

    /**
     * 구독자 수 조회
     */
    @GetMapping("/subscribers/count")
    public ResponseEntity<Long> getSubscribersCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // FIXME: CustomUserDetails 필드 확인 후 수정
        Long userId = userDetails.getUserId();

        Long subscriberCount
                = adminService.getSubscribersCount(userId);

        return ResponseEntity.ok().body(subscriberCount);
    }

    /**
     * 매물 수 조회
     */
    @GetMapping("/listings/count")
    public ResponseEntity<Long> getListingsCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // FIXME: CustomUserDetails 필드 확인 후 수정
        Long userId = userDetails.getUserId();

        Long listingCount
                = adminService.getListingsCount(userId);

        return ResponseEntity.ok().body(listingCount);
    }
}
