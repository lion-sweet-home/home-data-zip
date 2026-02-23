package org.example.homedatazip.global.batch.apartment.reader;

import jakarta.persistence.EntityManagerFactory;
import org.example.homedatazip.apartment.entity.Apartment;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 아파트-학교 거리 배치용 Reader. 위/경도가 있는 아파트를 2000건씩 페이징 조회. */
@Configuration
public class ApartmentSchoolDistanceReader {

    private static final int PAGE_SIZE = 500;

    @Bean
    @StepScope
    public JpaPagingItemReader<Apartment> apartmentSchoolDistanceItemReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<Apartment>()
                .name("apartmentSchoolDistanceReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                        SELECT a FROM Apartment a
                        WHERE a.latitude IS NOT NULL AND a.longitude IS NOT NULL
                        ORDER BY a.id
                        """)
                .pageSize(PAGE_SIZE)
                .build();
    }
}
