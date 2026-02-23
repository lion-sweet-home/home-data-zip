package org.example.homedatazip.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
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
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * 이번 달 수입 조회
     */
    @GetMapping("/monthly-income")
    public ResponseEntity<Long> getMonthlyIncome(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long monthlyIncome
                = adminService.getMonthlyIncome(userDetails.getUserId());

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
        List<SettlementResponse> yearlyIncome
                = adminService.getYearlyIncome(userDetails.getUserId(), year);

        return ResponseEntity.ok().body(yearlyIncome);
    }

    /**
     * 총 회원 수 조회
     */
    @GetMapping("/users/count")
    public ResponseEntity<Long> getUsersCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userCount
                = adminService.getUserCount(userDetails.getUserId());

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
        Page<UserResponse> usersList
                = adminService.getUserList(userDetails.getUserId(), pageable);

        return ResponseEntity.ok().body(usersList);
    }

    /**
     * 구독자 수 조회
     */
    @GetMapping("/subscribers/count")
    public ResponseEntity<Long> getSubscribersCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long subscriberCount
                = adminService.getSubscribersCount(userDetails.getUserId());

        return ResponseEntity.ok().body(subscriberCount);
    }

    /**
     * 매물 수 조회
     */
    @GetMapping("/listings/count")
    public ResponseEntity<Long> getListingsCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long listingCount
                = adminService.getListingsCount(userDetails.getUserId());

        return ResponseEntity.ok().body(listingCount);
    }

    /**
     * 스케줄러 수동 실행 (테스트용)
     * 특정 날짜의 결제 내역을 정산에 반영
     */
    @PostMapping("/settlement/process")
    public ResponseEntity<String> processSettlement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String date
    ) {
        // 관리자 검증은 Service에서 처리됨
        adminService.processSettlement(userDetails.getUserId(), LocalDate.parse(date));
        return ResponseEntity.ok("정산 처리 완료: " + date);
    }
}
