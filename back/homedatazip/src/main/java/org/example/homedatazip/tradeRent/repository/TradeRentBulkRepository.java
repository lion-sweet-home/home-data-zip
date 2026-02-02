package org.example.homedatazip.tradeRent.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

/**
 * TradeRent 대량 삽입을 위한 Repository
 * INSERT IGNORE를 사용하여 중복 데이터 자동 스킵
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TradeRentBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_IGNORE_SQL = """
            INSERT IGNORE INTO trade_rent 
            (apartment_id, deposit, monthly_rent, exclusive_area, floor, deal_date, renewal_requested, rent_term, sgg_code)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /**
     * INSERT IGNORE로 대량 삽입
     * 중복 데이터는 자동으로 무시됨 (예외 발생 X)
     *
     * @param tradeRents 삽입할 TradeRent 리스트
     * @return [삽입 성공 건수, 중복 스킵 건수]
     */
    public int[] bulkInsertIgnore(List<TradeRent> tradeRents) {
        if (tradeRents == null || tradeRents.isEmpty()) {
            return new int[]{0, 0};
        }

        int[][] batchResults = jdbcTemplate.batchUpdate(INSERT_IGNORE_SQL, tradeRents, tradeRents.size(),
                (ps, rent) -> {
                    // apartment_id (null 허용)
                    if (rent.getApartment() != null && rent.getApartment().getId() != null) {
                        ps.setLong(1, rent.getApartment().getId());
                    } else {
                        ps.setNull(1, java.sql.Types.BIGINT);
                    }

                    ps.setLong(2, rent.getDeposit());
                    ps.setInt(3, rent.getMonthlyRent());
                    ps.setDouble(4, rent.getExclusiveArea());
                    ps.setInt(5, rent.getFloor());
                    ps.setDate(6, Date.valueOf(rent.getDealDate()));

                    // renewal_requested (null 허용)
                    if (rent.getRenewalRequested() != null) {
                        ps.setBoolean(7, rent.getRenewalRequested());
                    } else {
                        ps.setNull(7, java.sql.Types.BOOLEAN);
                    }

                    ps.setString(8, rent.getRentTerm());
                    ps.setString(9, rent.getSggCd());
                });

        // 결과 분석
        int insertedCount = 0;
        int skippedCount = 0;

        for (int[] batch : batchResults) {
            for (int result : batch) {
                if (result > 0) {
                    insertedCount++;
                } else {
                    // INSERT IGNORE에서 중복으로 스킵된 경우 0 반환
                    skippedCount++;
                }
            }
        }

        log.info("TradeRent Bulk Insert - 시도: {}건, 저장: {}건, 중복 스킵: {}건",
                tradeRents.size(), insertedCount, skippedCount);

        return new int[]{insertedCount, skippedCount};
    }

    /**
     * ON DUPLICATE KEY UPDATE로 Upsert
     * 중복 시 기존 데이터 업데이트
     */
    public int[] bulkUpsert(List<TradeRent> tradeRents) {
        if (tradeRents == null || tradeRents.isEmpty()) {
            return new int[]{0, 0};
        }

        String upsertSql = """
                INSERT INTO trade_rent 
                (apartment_id, deposit, monthly_rent, exclusive_area, floor, deal_date, renewal_requested, rent_term, sgg_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    renewal_requested = VALUES(renewal_requested),
                    rent_term = VALUES(rent_term)
                """;

        int[][] batchResults = jdbcTemplate.batchUpdate(upsertSql, tradeRents, tradeRents.size(),
                (ps, rent) -> {
                    if (rent.getApartment() != null && rent.getApartment().getId() != null) {
                        ps.setLong(1, rent.getApartment().getId());
                    } else {
                        ps.setNull(1, java.sql.Types.BIGINT);
                    }

                    ps.setLong(2, rent.getDeposit());
                    ps.setInt(3, rent.getMonthlyRent());
                    ps.setDouble(4, rent.getExclusiveArea());
                    ps.setInt(5, rent.getFloor());
                    ps.setDate(6, Date.valueOf(rent.getDealDate()));

                    if (rent.getRenewalRequested() != null) {
                        ps.setBoolean(7, rent.getRenewalRequested());
                    } else {
                        ps.setNull(7, java.sql.Types.BOOLEAN);
                    }

                    ps.setString(8, rent.getRentTerm());
                    ps.setString(9, rent.getSggCd());
                });

        int insertedCount = 0;
        int updatedCount = 0;

        for (int[] batch : batchResults) {
            for (int result : batch) {
                if (result == 1) {
                    insertedCount++;
                } else if (result == 2) {
                    // ON DUPLICATE KEY UPDATE 시 2 반환
                    updatedCount++;
                }
            }
        }

        log.info("TradeRent Bulk Upsert - 시도: {}건, 신규: {}건, 업데이트: {}건",
                tradeRents.size(), insertedCount, updatedCount);

        return new int[]{insertedCount, updatedCount};
    }
}
