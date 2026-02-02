package org.example.homedatazip.global.batch.apartment.partition;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.repository.RegionRepository;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ApartmentIdPartitioner implements Partitioner {

    private final RegionRepository regionRepository;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();
        List<String> lawdCodes = regionRepository.findAll().stream()
                .map(Region::getSggCode)
//                .filter(code -> code != null && code.startsWith("11")) // 서울(11) 필터 추가
                .distinct().toList();

        List<String> dealYmds = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 1; i < 6; i++) {
            dealYmds.add(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM")));
        }

        int number = 0;
        for (String lawdCd : lawdCodes) {
            for (String dealYmd : dealYmds) {
                ExecutionContext value = new ExecutionContext();
                value.putString("lawdCd", lawdCd);
                value.putString("dealYmd", dealYmd);
                result.put("partition" + number++, value);
            }
        }
        return result;
    }
}
