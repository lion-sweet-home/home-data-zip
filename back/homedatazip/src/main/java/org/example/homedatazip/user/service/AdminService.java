package org.example.homedatazip.user.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.validation.RoleValidation;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.settlement.dto.SettlementResponse;
import org.example.homedatazip.settlement.entity.Settlement;
import org.example.homedatazip.settlement.repository.SettlementRepository;
import org.example.homedatazip.settlement.scheduler.SettlementScheduler;
import org.example.homedatazip.user.dto.UserResponse;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoleValidation roleValidation;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final SettlementRepository settlementRepository;

    /**
     * 이번 달 수입 조회
     */
    @Transactional(readOnly = true)
    public Long getMonthlyIncome(Long userId) {

        roleValidation.validateAdmin(userId);

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        return settlementRepository
                .findByPeriodStartAndPeriodEnd(startOfMonth, endOfMonth)
                .map(Settlement::getAmount)
                .orElse(0L);
    }

    /**
     * 월별 수입 조회
     */
    @Transactional(readOnly = true)
    public List<SettlementResponse> getYearlyIncome(
            Long userId,
            int year
    ) {

        roleValidation.validateAdmin(userId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Settlement> settlements = settlementRepository
                .findByPeriodStartBetweenOrderByPeriodStartAsc(startDate, endDate);

        return settlements.stream()
                .map(SettlementResponse::from)
                .toList();
    }

    /**
     * 총 회원 수 조회
     * <br/>
     * ADMIN을 제외한 모든 회원 수 조회
     */
    @Transactional(readOnly = true)
    public Long getUserCount(Long userId) {

        roleValidation.validateAdmin(userId);

        return userRepository.countByRoleTypeNot(RoleType.ADMIN);
    }

    /**
     * 회원 목록 조회
     * <br/>
     * ADMIN을 제외한 전체 유저를 페이징 처리하여 조회
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUserList(
            Long userId,
            Pageable pageable
    ) {

        roleValidation.validateAdmin(userId);

        Page<User> nonAdminUsers
                = userRepository.findByRoleTypeNot(RoleType.ADMIN, pageable);

        return nonAdminUsers.map(UserResponse::from);
    }

    /**
     * 구독자 수 조회
     * <br/>
     * Role이 SELLER인 회원 수 조회
     */
    @Transactional(readOnly = true)
    public Long getSubscribersCount(Long userId) {

        roleValidation.validateAdmin(userId);

        return userRepository.countByRoleType(RoleType.SELLER);
    }

    /**
     * 매물 수 조회
     * <br/>
     * 등록된 모든 매물 수 조회
     */
    @Transactional(readOnly = true)
    public Long getListingsCount(Long userId) {

        roleValidation.validateAdmin(userId);

        return listingRepository.count();
    }

    /**
     * 수동 정산 (테스트용)
     */
    private final SettlementScheduler settlementScheduler;

    @Transactional
    public void processSettlement(Long userId, LocalDate targetDate) {
        roleValidation.validateAdmin(userId);
        settlementScheduler.processSettlement(targetDate);
    }
}
