package org.example.homedatazip.global.batch.busstation.config;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse.Row;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.batch.BusStationProcessor;
import org.example.homedatazip.global.batch.busstation.reader.BusStationApiReader;
import org.example.homedatazip.global.batch.busstation.writer.BusStationWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class BusStationBatchConfig {

    private final BusStationApiReader reader;
    private final BusStationProcessor processor;
    private final BusStationWriter writer;

    @Bean
    public Step busStationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("busStationStep", jobRepository)
                .<SeoulBusStopResponse.Row, BusStation>chunk(200, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
