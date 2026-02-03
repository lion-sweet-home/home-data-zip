package org.example.homedatazip.global.batch.subwaystation.tasklet;

import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.subway.entity.SubwayStation;
import org.example.homedatazip.subway.entity.SubwayStationSource;
import org.example.homedatazip.subway.repository.SubwayStationRepository;
import org.example.homedatazip.subway.repository.SubwayStationSourceRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step2: subway_station_sources → 역명(stationName) 그룹 → 위/경도 중앙값 → SubwayStation upsert + 역지오코딩(Region) + Source.station_id 매핑
 */
@Slf4j
@Component
public class SubwayStationMapTasklet implements Tasklet {

    private final SubwayStationSourceRepository sourceRepository;
    private final SubwayStationRepository stationRepository;
    private final GeoService geoService;

    public SubwayStationMapTasklet(
            SubwayStationSourceRepository sourceRepository,
            SubwayStationRepository stationRepository,
            GeoService geoService
    ) {
        this.sourceRepository = sourceRepository;
        this.stationRepository = stationRepository;
        this.geoService = geoService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<SubwayStationSource> allSources = sourceRepository.findAllByOrderByStationNameAscIdAsc();
        Map<String, List<SubwayStationSource>> byStationName = allSources.stream()
                .collect(Collectors.groupingBy(SubwayStationSource::getStationName));

        for (Map.Entry<String, List<SubwayStationSource>> entry : byStationName.entrySet()) {
            String stationName = entry.getKey();
            List<SubwayStationSource> sources = entry.getValue();
            if (sources.isEmpty()) continue;

            List<Double> lats = sources.stream().map(SubwayStationSource::getLatitude).toList();
            List<Double> lons = sources.stream().map(SubwayStationSource::getLongitude).toList();
            double medianLat = median(lats);
            double medianLon = median(lons);

            SubwayStation station = stationRepository.findByStationName(stationName)
                    .orElseGet(SubwayStation::new);
            station.setStationName(stationName);
            station.setLatitude(medianLat);
            station.setLongitude(medianLon);
            station = stationRepository.save(station);

            // 대표 위/경도 → 역지오코딩(REQUIRES_NEW) → Region 설정 (실패 시 region=null 유지, Step 트랜잭션 유지)
            try {
                Region region = geoService.convertAddressInfoInNewTransaction(medianLat, medianLon);
                station.setRegion(region);
                stationRepository.save(station);
            } catch (Exception e) {
                log.warn("역지오코딩 실패 - 역명={}, lat={}, lon={}: {}", stationName, medianLat, medianLon, e.getMessage());
            }

            for (SubwayStationSource source : sources) {
                source.setStation(station);
            }
            sourceRepository.saveAll(sources);
        }

        return RepeatStatus.FINISHED;
    }

    private static double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("위/경도 값이 비어있음");
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int size = sorted.size();
        if (size == 1) return sorted.get(0);
        int mid = size / 2;
        if (size % 2 == 1) return sorted.get(mid);
        return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }
}
