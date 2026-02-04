package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

import java.util.List;

public record MonthDepositAvgResponse (
        List<MonthDeposit> monthDeposits
){
    public static MonthDepositAvgResponse map(List<MonthAvg> monthAvgs){
        List<MonthDeposit> list = monthAvgs.stream()
                .map(MonthDeposit::from)
                .toList();
        return new MonthDepositAvgResponse(list);
    }
    public record MonthDeposit(
            Long id,
            Long aptId,
            Double exclusive,
            String yyyymm,
            Long jeonseAvg,
            Long WolseAvg,
            Long wolseRentAvg,
            Integer jeonseCount,
            Integer wolseCount
    ){
        public static MonthDeposit from(MonthAvg monthAvg){
            int jc = nz(monthAvg.getJeonseCount());
            int wc = nz(monthAvg.getWolseCount());

            long jeonseAvg = avg(nz(monthAvg.getJeonseDepositSum()), jc);
            long wolseAvg = avg(nz(monthAvg.getWolseDepositSum()), wc);
            long wolseRentAvg = avg(nz(monthAvg.getWolseRentSum()), wc);

            return new MonthDeposit(
                    monthAvg.getId(),
                    monthAvg.getAptId(),
                    (monthAvg.getAreaTypeId() % 1_000_000) / 10.0,
                    monthAvg.getYyyymm(),
                    jeonseAvg,
                    wolseAvg,
                    wolseRentAvg,
                    jc,
                    wc
            );
        }
        private static long avg(long sum, int count) {
            return count <= 0 ? 0L : (sum / count); // 내림. 반올림 원하면 여기만 바꾸면 됨.
        }

        private static int nz(Integer v) {
            return v == null ? 0 : v;
        }

        private static long nz(Long v) {
            return v == null ? 0L : v;
        }
    }
}
