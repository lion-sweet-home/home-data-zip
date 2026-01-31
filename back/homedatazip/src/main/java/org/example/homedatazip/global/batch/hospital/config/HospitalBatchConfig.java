package org.example.homedatazip.global.batch.hospital.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.dto.HospitalApiResponse;
import org.example.homedatazip.hospital.entity.Hospital;
import org.example.homedatazip.hospital.repository.HospitalRepository;
import org.example.homedatazip.global.batch.hospital.service.HospitalApiClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Iterator;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HospitalBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final HospitalApiClient hospitalApiClient;
    private final HospitalRepository hospitalRepository;

    /**
     * Job: 배치 작업의 전체 단위
     * 여러 Step으로 구성될 수도 있다.
     */
    @Bean
    public Job hospitalImportJob() {
        return new JobBuilder("hospitalImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(hospitalImportStep())
                .build();
    }

    /**
     * Step: 실제 데이터 처리 단위
     * Reader -> Processor -> Writer 순서로 실행
     */
    @Bean
    public Step hospitalImportStep() {
        return new StepBuilder("hospitalImportStep", jobRepository)
                .<HospitalApiResponse.HospitalItem, Hospital>chunk(1000, transactionManager)
                .reader(hospitalItemReader()) // 데이터 읽기
                .processor(hospitalItemProcessor()) // 데이터 변환
                .writer(hospitalItemWriter()) // 데이터 저장
                .build();
    }

    /**
     * Reader: Open API에서 데이터 읽기
     * <br/>
     * read() 메서드는 데이터가 더 이상 없을 때까지 계속 호출
     * null을 반환하면 "더 이상 데이터 없음"으로 인식하여 종료
     */
    @Bean
    public ItemReader<HospitalApiResponse.HospitalItem> hospitalItemReader() {
        return new ItemReader<>() {

            private Iterator<HospitalApiResponse.HospitalItem> iterator;
            private int currentPage = 1;
            private int totalCount = -1;
            private int processedCount = 0;
            private final int pageSize = 1000;

            @Override
            public HospitalApiResponse.HospitalItem read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                // 첫 호출 시 전체 건수 조회
                if (totalCount == -1) {
                    totalCount = hospitalApiClient.getTotalCount();
                    log.info("전체 데이터 건수: {}", totalCount);
                }

                // 현재 페이지 소진 시 다음 페이지 로드
                if (iterator == null || !iterator.hasNext()) {
                    // 모든 데이터 처리 완료 체크
                    if (processedCount >= totalCount) {
                        log.info("모든 데이터 처리 완료: {}", processedCount);
                        return null; // 종료 신호
                    }

                    log.info("{} 페이지 로딩 중... (pageNo={}, numOfRows={})",
                            currentPage,
                            currentPage,
                            pageSize
                    );

                    // API 호출
                    HospitalApiResponse response
                            = hospitalApiClient.fetchHospital(currentPage, pageSize);

                    if (!response.isSuccess()) {
                        log.error("API 응답 오류: {}", response.getHeader().getResultMsg());
                        return null;
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
        };
    }

    /**
     * Processor: API 응답 -> Entity 변환
     * <br/>
     * Reader에서 읽은 데이터를 가공 혹은 필터링
     */
    @Bean
    public ItemProcessor<HospitalApiResponse.HospitalItem, Hospital> hospitalItemProcessor() {
        return row -> {
            // 필수 데이터 검증
            if (row.getHospitalId() == null || row.getName() == null) {
                log.warn("데이터 누락- ID: {}, 이름: {}",
                        row.getHospitalId(),
                        row.getName()
                );
                return null; // 해당 데이터는 저장하지 않음
            }

            return Hospital.fromApiResponse(
                    row.getHospitalId(),
                    row.getName(),
                    row.getTypeName(),
                    row.getAddress(),
                    row.getLatitude(),
                    row.getLongitude()
            );
        };
    }

    /**
     * Writer: DB에 저장
     * <br/>
     * Processor에서 가공한 데이터를 chunk 크기만큼 모아 한 번에 DB에 저장
     */
    @Bean
    public ItemWriter<Hospital> hospitalItemWriter() {
        return items -> {
            log.info("{} 건 저장 중", items.size());
            hospitalRepository.saveAll(items);
        };
    }
}
