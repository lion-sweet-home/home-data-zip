package org.example.homedatazip.global.batch.tradeRent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.global.batch.tradeRent.processor.TradeProcessor;
import org.example.homedatazip.global.batch.tradeRent.reader.TradeRentReader;
import org.example.homedatazip.global.batch.tradeRent.writer.TradeRentWriter;
import org.example.homedatazip.tradeRent.api.RentApiClient;
import org.example.homedatazip.tradeRent.dto.RentApiItem;
import org.example.homedatazip.tradeRent.dto.TradeRentWriteRequest;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TradeRentBackfillJobConfig {

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");
    private final TradeProcessor tradeProcessor;

    @Bean
    public Job tradeRentBackfillJob(JobRepository jobRepository, Step tradeRentBackfillStep ) {
        return new JobBuilder("tradeRentBackfillJob", jobRepository)
                .start(tradeRentBackfillStep)
                .build();
    }

    @Bean
    public Step tradeRentBackfillStep(JobRepository jobRepository,
                                    PlatformTransactionManager tx,
                                    ItemReader<RentApiItem> tradeRentBackfillReader,
                                    ItemProcessor<RentApiItem, TradeRentWriteRequest> tradeRentBackfillProcessor,
                                    ItemWriter<TradeRentWriteRequest> tradeRentBackfillWriter){
        return new StepBuilder("tradeRentBackfillStep", jobRepository)
                .<RentApiItem, TradeRentWriteRequest>chunk(100,tx)
                .reader(tradeRentBackfillReader)
                .processor(tradeRentBackfillProcessor)
                .writer(tradeRentBackfillWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<RentApiItem> tradeRentBackfillReader(
            RentApiClient client,
            RegionRepository regionRepository,
            @Value("#{jobParameters['fromYmd']}") String fromYmd,
            @Value("#{jobParameters['toYmd']}") String toYmd
    ) {

        String from = (fromYmd == null || fromYmd.isBlank()) ? "202201" : fromYmd;
        String to = (toYmd == null || toYmd.isBlank())
                ? java.time.YearMonth.now(java.time.ZoneId.of("Asia/Seoul")).minusMonths(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))
                : toYmd;

        List<String> sggCds = regionRepository.findDistinctSggCode();
        List<String> dealYmds = buildDealYmds(from, to);

        return new TradeRentReader(client, sggCds, dealYmds);
    }

    @Bean
    public ItemProcessor<RentApiItem, TradeRentWriteRequest> tradeRentBackfillProcessor() {
        return  tradeProcessor;
    }
    @Bean
    public ItemWriter<TradeRentWriteRequest> tradeRentBackfillWriter(TradeRentWriter writer){
        return writer;
    }

    private static List<String> buildDealYmds(String fromYmd, String toYmd) {
        YearMonth start = YearMonth.parse(fromYmd, YYYYMM);
        YearMonth end = YearMonth.parse(toYmd, YYYYMM);

        List<String> out = new ArrayList<>();
        YearMonth cur = start;
        while (!cur.isAfter(end)) {
            out.add(cur.format(YYYYMM));
            cur = cur.plusMonths(1);
        }
        return out;
    }
}
