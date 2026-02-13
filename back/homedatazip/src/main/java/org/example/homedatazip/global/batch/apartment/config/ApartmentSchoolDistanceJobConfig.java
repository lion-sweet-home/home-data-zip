package org.example.homedatazip.global.batch.apartment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSchoolDistance;
import org.example.homedatazip.apartment.repository.ApartmentSchoolDistanceRepository;
import org.example.homedatazip.global.batch.apartment.processor.ApartmentSchoolDistanceProcessor;
import org.example.homedatazip.global.batch.apartment.writer.ApartmentSchoolDistanceWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/** 아파트–학교 거리(하버사인 10km 이내) 중간 테이블 적재 Job. 기존 데이터 삭제 후 재적재 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApartmentSchoolDistanceJobConfig {

    private static final int CHUNK_SIZE = 2000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApartmentSchoolDistanceRepository apartmentSchoolDistanceRepository;
    private final JpaPagingItemReader<Apartment> apartmentSchoolDistanceItemReader;
    private final ApartmentSchoolDistanceProcessor apartmentSchoolDistanceProcessor;
    private final ApartmentSchoolDistanceWriter apartmentSchoolDistanceWriter;

    @Bean
    public Job apartmentSchoolDistanceJob() {
        return new JobBuilder("apartmentSchoolDistanceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(apartmentSchoolDistanceDeleteStep())
                .next(apartmentSchoolDistanceChunkStep())
                .build();
    }

    @Bean
    public Step apartmentSchoolDistanceDeleteStep() {
        return new StepBuilder("apartmentSchoolDistanceDeleteStep", jobRepository)
                .tasklet(apartmentSchoolDistanceDeleteTasklet(), transactionManager)
                .build();
    }

    private Tasklet apartmentSchoolDistanceDeleteTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[아파트-학교 거리] 기존 데이터 삭제 시작 (deleteAllInBatch)");
            apartmentSchoolDistanceRepository.deleteAllInBatch();
            log.info("[아파트-학교 거리] 기존 데이터 삭제 완료");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step apartmentSchoolDistanceChunkStep() {
        return new StepBuilder("apartmentSchoolDistanceChunkStep", jobRepository)
                .<Apartment, List<ApartmentSchoolDistance>>chunk(CHUNK_SIZE, transactionManager)
                .reader(apartmentSchoolDistanceItemReader)
                .processor(apartmentSchoolDistanceProcessor)
                .writer(apartmentSchoolDistanceWriter)
                .build();
    }
}