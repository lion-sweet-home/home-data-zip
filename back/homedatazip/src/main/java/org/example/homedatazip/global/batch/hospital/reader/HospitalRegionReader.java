package org.example.homedatazip.global.batch.hospital.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.entity.Hospital;
import org.example.homedatazip.hospital.repository.HospitalRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class HospitalRegionReader implements ItemReader<Hospital> {

    private final HospitalRepository hospitalRepository;
    private Iterator<Hospital> iterator;

    @Override
    public Hospital read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // iterator 소진 시 다음 조회
        if (iterator == null || !iterator.hasNext()) {
            List<Hospital> hospitals
                    = hospitalRepository
                    .findByRegionIsNullAndLatitudeIsNotNullAndLongitudeIsNotNull();

            if (hospitals.isEmpty()) {
                log.info("✅ Region 매칭 대상이 더 이상 없습니다.");
                return null;
            }

            log.info("📄 Region 미매칭 {} 건 로드", hospitals.size());
            iterator = hospitals.iterator();
        }

        return iterator.hasNext() ? iterator.next() : null;
    }
}
