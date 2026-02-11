package org.example.homedatazip.global.batch.hospital.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.entity.Hospital;
import org.example.homedatazip.hospital.repository.HospitalRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalRegionWriter implements ItemWriter<Hospital> {

    private final HospitalRepository hospitalRepository;

    @Override
    public void write(Chunk<? extends Hospital> items) throws Exception {
        log.info("💾 Region 매칭 {} 건 저장", items.size());
        hospitalRepository.saveAll(items.getItems());
    }
}
