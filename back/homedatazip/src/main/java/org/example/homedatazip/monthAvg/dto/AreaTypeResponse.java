package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;


import java.util.List;

public record AreaTypeResponse (
        Long aptId,
        List<AreaOption> options
){
    private record AreaOption(
            Long areaKey,
            Double exclusive
    ){

        private static AreaOption from(Long m){
            return new AreaOption(getAreaKey(m), getExclusive(m)

            );
        }
    }

    public static AreaTypeResponse map(List<Long> areaTypeIds, Long aptId){
        return new AreaTypeResponse(
                aptId,
                areaTypeIds.stream().map(AreaOption::from).toList()
        );
    }
    private static Long getAreaKey(Long areaTypeId){
        return areaTypeId % 1_000_000;
    }

    private static Double getExclusive(Long areaTypeId){
        double v =  getAreaKey(areaTypeId) / 100.0;
        return v;
    }
}
