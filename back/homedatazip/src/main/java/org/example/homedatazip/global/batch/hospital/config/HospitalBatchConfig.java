package org.example.homedatazip.global.batch.hospital.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BatchSkipException;
import org.example.homedatazip.global.geocode.service.GeoService;
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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final GeoService geoService;

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
                .faultTolerant() // 내결함성 기능 활성화
                .skipLimit(100) // 최대 100건 오류 허용
                .skip(BatchSkipException.class) // Custom Exception 발생 시 스킵
                .skip(DataIntegrityViolationException.class) // 중복 키 오류 스킵
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
                log.warn("⚠️ 데이터 누락- ID: {}, 이름: {}",
                        row.getHospitalId(),
                        row.getName()
                );
                return null; // 해당 데이터는 저장하지 않음
            }

            // 지역 필터링 (서울, 인천, 경기만 저장)
            if (!isTargetRegion(row.getAddress())) {
                log.debug("⏭️ 저장하지 않는 지역 - {}", row.getAddress());
                return null; // 해당 데이터는 저장하지 않음
            }

            // Region 조회 (서울, 인천, 경기 필터링 통과된 병원 한정)
            Region region = null;

            if (row.getLatitude() != null && row.getLongitude() != null) {
                try {
                    region = geoService.convertAddressInfoInNewTransaction(
                            row.getLatitude(),
                            row.getLongitude()
                    );
                } catch (BatchSkipException e) {
                    // BatchSkipException 발생 시 예외를 잡아 null을 반환
                    log.warn("⏭️ Region 조회 실패 (스킵) - Hospital: {}, 사유: {}",
                            row.getName(),
                            e.getMessage()
                    );
                    return null; // null 반환으로 필터링
                }
            }

            // Region 조회 실패 시 로그
            if (region == null) {
                log.warn("⚠️ Region 조회 실패 - Hospital: {}, 위도: {}, 경도: {}",
                        row.getName(),
                        row.getLatitude(),
                        row.getLongitude()
                );
            }

            return Hospital.fromApiResponse(
                    row.getHospitalId(),
                    row.getName(),
                    row.getTypeName(),
                    region,
                    row.getAddress(),
                    row.getLatitude(),
                    row.getLongitude()
            );
        };
    }

    /**
     * 서울, 인천, 경기 필터링
     */
    private boolean isTargetRegion(String address) {
        if (address == null || address.isEmpty()) return false;

        return address.startsWith("서울") ||
               address.startsWith("인천") ||
               address.startsWith("경기");
    }

    /**
     * Writer: DB에 저장
     * <br/>
     * Processor에서 가공한 데이터를 chunk 크기만큼 모아 한 번에 DB에 저장
     * UPSERT 방식 (기존에 존재하면 UPDATE, 존재하지 않으면 INSERT)
     */
    @Bean
    public ItemWriter<Hospital> hospitalItemWriter() {
        return items -> {
            log.info("💾 {} 건 저장/업데이트 중", items.size());

            for (Hospital hospital : items) {
                hospitalRepository.findByHospitalId(hospital.getHospitalId())
                        .ifPresentOrElse(
                                existing -> {
                                    // 이미 존재하면 업데이트
                                    existing.updateFrom(
                                            hospital.getName(),
                                            hospital.getTypeName(),
                                            hospital.getRegion(),
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
        };
    }
}
