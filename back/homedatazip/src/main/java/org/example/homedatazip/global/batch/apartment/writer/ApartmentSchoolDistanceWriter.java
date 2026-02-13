package org.example.homedatazip.global.batch.apartment.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.ApartmentSchoolDistance;
import org.example.homedatazip.apartment.repository.ApartmentSchoolDistanceRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

/** Processor 출력(List<List<ApartmentSchoolDistance>>)을 flatten 후 bulk 저장 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApartmentSchoolDistanceWriter implements ItemWriter<List<ApartmentSchoolDistance>> {

    private static final int SAVE_BATCH_SIZE = 5000;
    private static final int PROGRESS_LOG_EVERY_BATCH = 10;
    private long savedTotal = 0;

    private final ApartmentSchoolDistanceRepository apartmentSchoolDistanceRepository;

    @Override
    public void write(Chunk<? extends List<ApartmentSchoolDistance>> chunk) throws Exception {
        List<ApartmentSchoolDistance> flat = chunk.getItems().stream()
                .flatMap(list -> list.stream())
                .toList();

        if (flat.isEmpty()) {
            return;
        }

        for (int i = 0; i < flat.size(); i += SAVE_BATCH_SIZE) {
            int end = Math.min(i + SAVE_BATCH_SIZE, flat.size());
            List<ApartmentSchoolDistance> batch = flat.subList(i, end);
            try {
                apartmentSchoolDistanceRepository.saveAll(batch);
                savedTotal += batch.size();
                if ((i / SAVE_BATCH_SIZE) % PROGRESS_LOG_EVERY_BATCH == 0) {
                    log.info("[아파트-학교 거리] 저장 진행: chunkItems={}, batchRange={}~{}, savedTotal(approx)={}",
                            flat.size(), i, end, savedTotal);
                }
            } catch (Exception e) {
                ApartmentSchoolDistance first = batch.getFirst();
                ApartmentSchoolDistance last = batch.getLast();
                log.error("[아파트-학교 거리] 저장 중 예외 발생. chunkItems={}, batchRange={}~{}, first(aptId={}, schoolId={}, distKm={}), last(aptId={}, schoolId={}, distKm={})",
                        flat.size(), i, end,
                        first.getApartment().getId(), first.getSchool().getId(), first.getDistanceKm(),
                        last.getApartment().getId(), last.getSchool().getId(), last.getDistanceKm(),
                        e);
                throw e;
            }
        }
        log.debug("[아파트-학교 거리] 청크 저장 완료: {}건", flat.size());
    }
}
