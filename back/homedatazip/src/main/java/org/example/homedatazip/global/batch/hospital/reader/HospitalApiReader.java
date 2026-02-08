package org.example.homedatazip.global.batch.hospital.reader;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.dto.HospitalApiResponse;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Reader: Open API에서 데이터 읽기
 * <br/>
 * read() 메서드는 데이터가 더 이상 없을 때까지 계속 호출
 * null을 반환하면 "더 이상 데이터 없음"으로 인식하여 종료
 */
@Slf4j
@Component
@StepScope
public class HospitalApiReader implements ItemReader<HospitalApiResponse.HospitalItem> {

    private final HospitalApiClient hospitalApiClient;

    private Iterator<HospitalApiResponse.HospitalItem> iterator;
    private int currentPage = 1;
    private int totalCount = -1;
    private int processedCount = 0;
    private final int pageSize = 1000;

    public HospitalApiReader(HospitalApiClient hospitalApiClient) {
        this.hospitalApiClient = hospitalApiClient;
    }

    @Override
    public HospitalApiResponse.HospitalItem read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // 현재 페이지 소진 시 다음 페이지 로드
        if (iterator == null || !iterator.hasNext()) {
            // 모든 데이터 처리 완료 체크
            if (processedCount >= totalCount && totalCount != -1) {
                log.info("✅ 모든 데이터 처리 완료: {}", processedCount);
                return null; // 종료 신호
            }

            log.info("📄 {} 페이지 로딩 중... (pageNo={}, numOfRows={})",
                    currentPage,
                    currentPage,
                    pageSize
            );

            // API 호출
            HospitalApiResponse response
                    = hospitalApiClient.fetchHospital(currentPage, pageSize);

            if (!response.isSuccess()) {
                log.error("🚨 API 응답 오류: {}", response.getHeader().getResultMsg());
                return null;
            }

            // 첫 호출 시 totalCount 설정
            if (totalCount == -1) {
                totalCount = response.getTotalCount();
                log.info("📊 전체 데이터 건수: {}", totalCount);
            }

            // 데이터가 없는 경우 종료
            if (response.getItems() == null || response.getItems().isEmpty()) {
                return null;
            }

            iterator = response.getItems().iterator();
            currentPage++;
        }

        processedCount++;
        return iterator.hasNext() ? iterator.next() : null;
    }
}
