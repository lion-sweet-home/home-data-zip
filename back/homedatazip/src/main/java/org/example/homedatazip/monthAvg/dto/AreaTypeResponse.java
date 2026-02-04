package org.example.homedatazip.monthAvg.dto;

import java.util.List;

public record AreaTypeResponse (
        Long aptId,
        List<AreaOption> options
){
    private record AreaOption(
            Long areaKey,
            Double exclusive
    ){

        private static AreaOption from(Long areaTypeIds){
            return new AreaOption(getAreaKey(areaTypeIds), getExclusive(areaTypeIds));
        }
    }

    public static AreaTypeResponse map(Long aptId, List<Long> areaTypeIds){
        return new AreaTypeResponse(
                aptId,
                areaTypeIds.stream().map(AreaOption::from).toList()
        );
    }
    private static Long getAreaKey(Long areaTypeId){
        return areaTypeId % 1_000_000;
    }

    private static Double getExclusive(Long areaTypeId){
        double v =  getAreaKey(areaTypeId) / 10.0;
        return v;
    }
}
