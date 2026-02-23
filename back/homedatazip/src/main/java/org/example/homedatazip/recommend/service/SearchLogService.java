package org.example.homedatazip.recommend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.example.homedatazip.monthAvg.repository.MonthAvgRepository;
import org.example.homedatazip.recommend.dto.UserPyeongClickRequest;
import org.example.homedatazip.recommend.entity.UserSearchLog;
import org.example.homedatazip.recommend.repository.UserSearchLogRepository;
import org.example.homedatazip.recommend.type.LogType;
import org.example.homedatazip.recommend.type.TradeType;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.repository.TradeRentDSLRepository;
import org.example.homedatazip.tradeSale.dto.SaleSearchRequest;
import org.example.homedatazip.tradeSale.dto.TradeSaleHistory;
import org.example.homedatazip.tradeSale.repository.TradeSaleQueryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchLogService {

    private final UserSearchLogRepository userSearchLogRepository;
    private final ApartmentRepository apartmentRepository;
    private final RegionRepository regionRepository;
    private final Map<Long, Long> lastRequestTimeMap = new ConcurrentHashMap<>();
    private final MonthAvgRepository monthAvgRepository;
    private final TradeSaleQueryRepository tradeSaleQueryRepository;
    private final TradeRentDSLRepository tradeRentDSLRepository;

    // 1. 매매 SEARCH 로그 저장
    @Async("searchLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchFilterLog(Long userId, SaleSearchRequest req) {
        if (isDuplicateRequest(userId)) return;

        try {
            String sggCode = getSggCode(req.sido(), req.gugun());

            UserSearchLog logEntity = UserSearchLog.createSearchLog(
                    userId, sggCode,
                    req.minAmount(), req.maxAmount(),
                    0L, 0L,
                    req.minArea(), req.maxArea(),
                    req.minBuildYear(), req.periodMonths(),
                    TradeType.SALE
            );
            userSearchLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("매매 검색 로그 저장 실패: {}", e.getMessage());
        }
    }

    // 2. 전월세 SEARCH 로그 저장
    @Async("searchLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRentSearchLog(Long userId, RentGetMarkerRequest req) {
        if (isDuplicateRequest(userId)) return;

        try {
            String sggCode = getSggCode(req.sido(), req.gugun());

            Long minMonthly = (req.minMonthlyRent() != null) ? req.minMonthlyRent().longValue() : 0L;
            Long maxMonthly = (req.maxMonthlyRent() != null) ? req.maxMonthlyRent().longValue() : 0L;

            TradeType type = (minMonthly > 0) ? TradeType.WOLSE : TradeType.RENT;

            UserSearchLog logEntity = UserSearchLog.createSearchLog(
                    userId, sggCode,
                    req.minDeposit(), req.maxDeposit(),
                    minMonthly, maxMonthly,
                    req.minExclusive(), req.maxExclusive(),
                    null, null,
                    type
            );
            userSearchLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("[RENT_LOG] 저장 실패: {}", e.getMessage());
        }
    }

    // 3. ACTION 로그 저장 (SUMMARY, DETAIL)
    @Async("searchLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveActionLog(Long userId, Long aptId, LogType logType, TradeType tradeType) {
        if (isDuplicateRequest(userId)) return;

        apartmentRepository.findById(aptId).ifPresent(apt -> {
            UserSearchLog logEntity = UserSearchLog.createActionLog(
                    userId, apt.getRegion().getSggCode(), tradeType, logType
            );
            userSearchLogRepository.save(logEntity);
        });
    }

    // 4. 평수 클릭 로그 저장
    @Async("searchLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePyeongClickLog(Long userId, UserPyeongClickRequest req) {
        apartmentRepository.findById(req.aptId()).ifPresent(apt -> {

            long finalPrice = 0L;
            long monthlyRent = 0L;

            TradeType currentType = (req.isRent() && (req.monthlyRent() == null || req.monthlyRent() == 0))
                    ? TradeType.RENT : TradeType.of(req.monthlyRent(), req.isRent());

            Long areaTypeId = null;

            if (currentType == TradeType.SALE) {
                List<TradeSaleHistory> histories = tradeSaleQueryRepository.findTradeHistory(req.aptId());
                areaTypeId = histories.stream()
                        .filter(h -> Math.abs(h.areaKey() - req.area()) < 0.1)
                        .map(TradeSaleHistory::areaTypeId)
                        .findFirst()
                        .orElse(null);
            } else {
                List<RentFromAptResponse> recentRents = tradeRentDSLRepository.findRecent5(req.aptId());

                long areaKey = recentRents.stream()
                        .filter(r -> Math.abs(r.exclusiveArea() - req.area()) < 0.1)
                        .map(r -> Math.round(r.exclusiveArea() * 100.0))
                        .findFirst()
                        .orElse(Math.round(req.area() * 100.0));

                areaTypeId = (apt.getId() * 1000000L) + areaKey;
            }

            log.info("[DEBUG_LOG] Apt: {}, Area: {}, GeneratedID: {}, Type: {}",
                    apt.getAptName(), req.area(), areaTypeId, currentType);

            if (areaTypeId != null) {
                Optional<MonthAvg> monthAvgOpt = monthAvgRepository.findTopByAptIdAndAreaTypeIdAndYyyymmBeforeOrderByYyyymmDesc(
                        req.aptId(), areaTypeId, "999912");

                if (monthAvgOpt.isPresent()) {
                    MonthAvg m = monthAvgOpt.get();
                    switch (currentType) {
                        case SALE -> finalPrice = (m.getSaleCount() != null && m.getSaleCount() > 0)
                                ? m.getSaleDealAmountSum() / m.getSaleCount() : 0L;
                        case RENT -> finalPrice = (m.getJeonseCount() != null && m.getJeonseCount() > 0)
                                ? m.getJeonseDepositSum() / m.getJeonseCount() : 0L;
                        case WOLSE -> {
                            finalPrice = (m.getWolseCount() != null && m.getWolseCount() > 0)
                                    ? m.getWolseDepositSum() / m.getWolseCount() : 0L;
                            monthlyRent = (m.getWolseCount() != null && m.getWolseCount() > 0)
                                    ? m.getWolseRentSum() / m.getWolseCount() : 0L;
                        }
                    }
                }
            }

            userSearchLogRepository.save(UserSearchLog.createClickLog(
                    userId,
                    apt.getId(),
                    apt.getRegion().getSggCode(),
                    req.area(),
                    finalPrice,
                    monthlyRent,
                    currentType
            ));
        });
    }

    private boolean isDuplicateRequest(Long userId) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastRequestTimeMap.getOrDefault(userId, 0L);
        if (currentTime - lastTime < 1000) {
            log.info("[LOG_SKIP] 중복 요청 건너뜀: User {}", userId);
            return true;
        }
        lastRequestTimeMap.put(userId, currentTime);
        return false;
    }

    private String getSggCode(String sido, String gugun) {
        return regionRepository.findRegionsBySidoAndGugun(sido, gugun)
                .stream()
                .findFirst()
                .map(Region::getSggCode)
                .orElse("00000");
    }
}