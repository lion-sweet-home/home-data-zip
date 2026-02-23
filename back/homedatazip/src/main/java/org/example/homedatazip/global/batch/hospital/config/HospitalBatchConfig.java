package org.example.homedatazip.global.batch.hospital.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.dto.HospitalApiResponse;
import org.example.homedatazip.hospital.entity.Hospital;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HospitalBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ItemReader<HospitalApiResponse.HospitalItem> hospitalApiReader;
    private final ItemProcessor<HospitalApiResponse.HospitalItem, Hospital> hospitalImportProcessor;
    private final ItemWriter<Hospital> hospitalUpsertWriter;

    private final ItemReader<Hospital> hospitalRegionReader;
    private final ItemProcessor<Hospital, Hospital> hospitalRegionProcessor;
    private final ItemWriter<Hospital> hospitalRegionWriter;

    /**
     * Job: 배치 작업의 전체 단위
     * 여러 Step으로 구성될 수도 있다.
     */
    @Bean
    public Job hospitalImportJob() {
        return new JobBuilder("hospitalImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(hospitalImportStep())
                .next(hospitalRegionStep())
                .build();
    }

    /**
     * Step: 실제 데이터 처리 단위
     * Reader -> Processor -> Writer 순서로 실행
     * <br/>
     * Step 1: Open API -> 지역 필터링 -> DB 저장
     */
    @Bean
    public Step hospitalImportStep() {
        return new StepBuilder("hospitalImportStep", jobRepository)
                .<HospitalApiResponse.HospitalItem, Hospital>chunk(1000, transactionManager)
                .reader(hospitalApiReader) // 데이터 읽기
                .processor(hospitalImportProcessor) // 데이터 변환
                .writer(hospitalUpsertWriter) // 데이터 저장
                .faultTolerant() // 내결함성 기능 활성화
                .skipLimit(100) // 최대 100건 오류 허용
                .skip(DataIntegrityViolationException.class) // 중복 키 오류 스킵
                .taskExecutor(regionTaskExecutor())
                .build();
    }

    /**
     * Step 2: Region이 null인 Hospital에 Region 매칭
     */
    @Bean
    public Step hospitalRegionStep(
    ) {
        return new StepBuilder("hospitalRegionStep", jobRepository)
                .<Hospital, Hospital>chunk(500, transactionManager)
                .reader(hospitalRegionReader)
                .processor(hospitalRegionProcessor)
                .writer(hospitalRegionWriter)
                .taskExecutor(regionTaskExecutor())
                .build();
    }

    /**
     * 스레드 풀을 사용하여 여러 스레드가 chunk를 동시에 처리
     */
    @Bean
    public TaskExecutor regionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("apt-batch-");
        executor.initialize();
        return executor;
    }
}
