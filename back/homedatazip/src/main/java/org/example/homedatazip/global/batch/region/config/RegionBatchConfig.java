package org.example.homedatazip.global.batch.region.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.dto.RegionApiResponse;
import org.example.homedatazip.data.service.RegionService;
import org.example.homedatazip.global.batch.region.processor.RegionProcessor;
import org.example.homedatazip.global.batch.region.reader.RegionApiReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RegionBatchConfig {
    private final RegionApiReader reader;
    private final RegionProcessor regionProcessor;
    private final RegionService regionService;

    @Bean
    public Job regionJob(Step regionStep, JobRepository jobRepository) {
        return new JobBuilder("regionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(regionStep)
                .build();
    }

    @Bean
    public Step regionStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("regionStep", jobRepository)
                .<RegionApiResponse, RegionApiResponse>chunk(100, transactionManager)
                .reader(reader)
                .processor(regionProcessor)
                .writer(regionItemWriter())
                .build();
    }

    @Bean
    public ItemWriter<RegionApiResponse> regionItemWriter() {
        return chunk -> {
            List<RegionApiResponse> items = new ArrayList<>(chunk.getItems());
            regionService.upsertRegions(items);
        };
    }

}
