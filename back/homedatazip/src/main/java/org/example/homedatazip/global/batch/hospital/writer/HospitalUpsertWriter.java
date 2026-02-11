package org.example.homedatazip.global.batch.hospital.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.entity.Hospital;
import org.example.homedatazip.hospital.repository.HospitalRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer: DB에 저장
 * <br/>
 * Processor에서 가공한 데이터를 chunk 크기만큼 모아 한 번에 DB에 저장
 * UPSERT 방식 (기존에 존재하면 UPDATE, 존재하지 않으면 INSERT)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalUpsertWriter implements ItemWriter<Hospital> {

    private final HospitalRepository hospitalRepository;

    @Override
    public void write(Chunk<? extends Hospital> items) throws Exception {
        log.info("💾 {} 건 저장/업데이트 중", items.size());

        for (Hospital hospital : items) {
            hospitalRepository.findByHospitalId(hospital.getHospitalId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.updateFrom(
                                        hospital.getName(),
                                        hospital.getTypeName(),
                                        existing.getRegion(), // 기존 Region 유지
                                        hospital.getAddress(),
                                        hospital.getLatitude(),
                                        hospital.getLongitude()
                                );
                                hospitalRepository.save(existing);
                            },
                            () -> {
                                // 없으면 새로 저장
                                hospitalRepository.save(hospital);
                            }
                    );
        }
    }
}
