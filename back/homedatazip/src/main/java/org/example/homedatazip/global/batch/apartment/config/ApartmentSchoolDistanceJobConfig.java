package org.example.homedatazip.global.batch.apartment.config;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.batch.apartment.tasklet.ApartmentSchoolDistanceTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** 아파트–학교 거리(하버사인 10km 이내) 중간 테이블 적재 Job */
@Configuration
@RequiredArgsConstructor
public class ApartmentSchoolDistanceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApartmentSchoolDistanceTasklet apartmentSchoolDistanceTasklet;

    @Bean
    public Job apartmentSchoolDistanceJob() {
        return new JobBuilder("apartmentSchoolDistanceJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(apartmentSchoolDistanceStep())
                .build();
    }

    @Bean
    public Step apartmentSchoolDistanceStep() {
        return new StepBuilder("apartmentSchoolDistanceStep", jobRepository)
                .tasklet(apartmentSchoolDistanceTasklet, transactionManager)
                .build();
    }
}