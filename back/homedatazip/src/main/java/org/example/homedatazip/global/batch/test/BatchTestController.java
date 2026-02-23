package org.example.homedatazip.global.batch.test;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.apartment.dto.ApiResponse;
import org.example.homedatazip.tradeRent.api.RentApiClient;
import org.example.homedatazip.tradeRent.dto.RentApiItem;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test/batch")
@RequiredArgsConstructor
public class BatchTestController {

    private final JobLauncher jobLauncher;

    @Qualifier("regionJob")
    private final Job regionJob;

    @Qualifier("tradeRentBackfillJob")
    private final Job tradeRentBackfillJob;

    private final RegionRepository regionRepository;
    private final ApartmentRepository apartmentRepository;
    private final TradeRentRepository tradeRentRepository;
    private final RentApiClient rentApiClient;

    @Value("${api.data-go-kr.service-key:NOT_SET}")
    private String regionServiceKey;

    @Value("${rent-api.service-key:NOT_SET}")
    private String rentApiServiceKey;

    @PostConstruct
    public void checkKey() {
        log.info("========== Service Key Check ==========");
        log.info("[Region API] first8={}, length={}",
                regionServiceKey == null ? "null" : regionServiceKey.substring(0, Math.min(8, regionServiceKey.length())),
                regionServiceKey == null ? -1 : regionServiceKey.length());
        log.info("[Rent API] first8={}, length={}",
                rentApiServiceKey == null ? "null" : rentApiServiceKey.substring(0, Math.min(8, rentApiServiceKey.length())),
                rentApiServiceKey == null ? -1 : rentApiServiceKey.length());
        log.info("========================================");
    }

    /**
     * Step 1: Region 배치 실행
     * Region 데이터를 API에서 가져와서 저장
     *
     * GET http://localhost:8080/api/test/batch/region
     */
    @PostMapping("/region")
    public ResponseEntity<Map<String, Object>> runRegionBatch() {
        Map<String, Object> result = new HashMap<>();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(regionJob, params);

            result.put("status", execution.getStatus().toString());
            result.put("jobId", execution.getJobId());
            result.put("message", "Region 배치 실행 완료");

            // 저장된 Region 수 확인
            long regionCount = regionRepository.count();
            result.put("totalRegions", regionCount);

            // 샘플 데이터 (처음 5개)
            List<Region> samples = regionRepository.findAll().stream().limit(5).toList();
            result.put("samples", samples.stream().map(r -> Map.of(
                    "id", r.getId(),
                    "sido", r.getSido(),
                    "gugun", r.getGugun(),
                    "dong", r.getDong(),
                    "lawdCode", r.getLawdCode(),
                    "sggCode", r.getSggCode()
            )).toList());

            log.info("Region 배치 완료 - 총 {}건 저장", regionCount);

        } catch (Exception e) {
            log.error("Region 배치 실행 실패", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 2: Region 저장 확인
     *
     * GET http://localhost:8080/api/test/batch/region/check
     */
    @GetMapping("/region/check")
    public ResponseEntity<Map<String, Object>> checkRegions() {
        Map<String, Object> result = new HashMap<>();

        long count = regionRepository.count();
        List<String> sggCodes = regionRepository.findDistinctSggCode();

        result.put("totalRegions", count);
        result.put("distinctSggCodes", sggCodes.size());
        result.put("sggCodeSamples", sggCodes.stream().limit(10).toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Step 3: TradeRent 배치 실행 (아파트 지오코딩 + 전월세 저장)
     *
     * POST http://localhost:8080/api/test/batch/trade-rent
     * Body: { "fromYmd": "202401", "toYmd": "202401" }
     *
     * 또는 쿼리 파라미터로:
     * POST http://localhost:8080/api/test/batch/trade-rent?fromYmd=202401&toYmd=202401
     */
    @PostMapping("/trade-rent")
    public ResponseEntity<Map<String, Object>> runTradeRentBatch(
            @RequestParam(defaultValue = "202401") String fromYmd,
            @RequestParam(defaultValue = "202401") String toYmd) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Region이 있는지 먼저 확인
            long regionCount = regionRepository.count();
            if (regionCount == 0) {
                result.put("status", "FAILED");
                result.put("error", "Region 데이터가 없습니다. 먼저 /api/test/batch/region 을 실행해주세요.");
                return ResponseEntity.badRequest().body(result);
            }

            JobParameters params = new JobParametersBuilder()
                    .addString("fromYmd", fromYmd)
                    .addString("toYmd", toYmd)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("TradeRent 배치 시작 - fromYmd: {}, toYmd: {}", fromYmd, toYmd);

            JobExecution execution = jobLauncher.run(tradeRentBackfillJob, params);

            result.put("status", execution.getStatus().toString());
            result.put("jobId", execution.getJobId());
            result.put("fromYmd", fromYmd);
            result.put("toYmd", toYmd);
            result.put("message", "TradeRent 배치 실행 완료");

            log.info("TradeRent 배치 완료 - Status: {}", execution.getStatus());

        } catch (Exception e) {
            log.error("TradeRent 배치 실행 실패", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 4: 저장된 데이터 확인 (Apartment + TradeRent)
     *
     * GET http://localhost:8080/api/test/batch/result
     */
    @GetMapping("/result")
    public ResponseEntity<Map<String, Object>> checkResults() {
        Map<String, Object> result = new HashMap<>();

        // Region 수
        long regionCount = regionRepository.count();
        result.put("totalRegions", regionCount);

        // Apartment 수
        long apartmentCount = apartmentRepository.count();
        result.put("totalApartments", apartmentCount);

        // TradeRent 수
        long tradeRentCount = tradeRentRepository.count();
        result.put("totalTradeRents", tradeRentCount);

        // Apartment 샘플 (처음 5개)
        List<Apartment> aptSamples = apartmentRepository.findAll().stream().limit(5).toList();
        result.put("apartmentSamples", aptSamples.stream().map(a -> Map.of(
                "id", a.getId(),
                "aptName", a.getAptName() != null ? a.getAptName() : "N/A",
                "aptSeq", a.getAptSeq(),
                "latitude", a.getLatitude() != null ? a.getLatitude() : 0,
                "longitude", a.getLongitude() != null ? a.getLongitude() : 0,
                "roadAddress", a.getRoadAddress() != null ? a.getRoadAddress() : "N/A"
        )).toList());

        // TradeRent 샘플 (처음 5개)
        List<TradeRent> rentSamples = tradeRentRepository.findAll().stream().limit(5).toList();
        result.put("tradeRentSamples", rentSamples.stream().map(r -> Map.of(
                "id", r.getId(),
                "deposit", r.getDeposit(),
                "monthlyRent", r.getMonthlyRent(),
                "dealDate", r.getDealDate().toString(),
                "floor", r.getFloor(),
                "exclusiveArea", r.getExclusiveArea()
        )).toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 전체 테스트 데이터 삭제 (초기화용)
     *
     * DELETE http://localhost:8080/api/test/batch/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearTestData() {
        Map<String, Object> result = new HashMap<>();

        try {
            long rentBefore = tradeRentRepository.count();
            long aptBefore = apartmentRepository.count();

            tradeRentRepository.deleteAll();
            apartmentRepository.deleteAll();

            result.put("message", "테스트 데이터 삭제 완료");
            result.put("deletedTradeRents", rentBefore);
            result.put("deletedApartments", aptBefore);

            log.info("테스트 데이터 삭제 - TradeRent: {}건, Apartment: {}건", rentBefore, aptBefore);

        } catch (Exception e) {
            log.error("데이터 삭제 실패", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Rent API 직접 호출 테스트 (Region 없이 API 키 유효성 확인)
     *
     * GET http://localhost:8080/api/test/batch/rent-api?sggCd=11110&dealYm=202401
     *
     * sggCd: 시군구 코드 (예: 11110 = 서울 종로구)
     * dealYm: 계약년월 (예: 202401)
     */
    @GetMapping("/rent-api")
    public ResponseEntity<Map<String, Object>> testRentApi(
            @RequestParam(defaultValue = "11110") String sggCd,
            @RequestParam(defaultValue = "202401") String dealYm,
            @RequestParam(defaultValue = "1") int pageNo) {

        Map<String, Object> result = new HashMap<>();
        result.put("sggCd", sggCd);
        result.put("dealYm", dealYm);
        result.put("pageNo", pageNo);

        log.info("========== Rent API 직접 호출 테스트 ==========");
        log.info("sggCd: {}, dealYm: {}, pageNo: {}", sggCd, dealYm, pageNo);

        try {
            ApiResponse<RentApiItem> response = rentApiClient.fetch(sggCd, dealYm, pageNo);

            if (response == null) {
                result.put("status", "FAILED");
                result.put("error", "API 응답이 null입니다.");
                return ResponseEntity.ok(result);
            }

            result.put("status", "SUCCESS");

            if (response.header() != null) {
                result.put("resultCode", response.header().resultCode());
                result.put("resultMsg", response.header().resultMsg());
            }

            if (response.body() != null) {
                result.put("totalCount", response.body().totalCount());
                result.put("numOfRows", response.body().numOfRows());
                result.put("pageNo", response.body().pageNo());

                if (response.body().items() != null && response.body().items().item() != null) {
                    List<RentApiItem> items = response.body().items().item();
                    result.put("itemCount", items.size());
                    // 샘플로 처음 3개만
                    result.put("sampleItems", items.stream().limit(3).toList());
                } else {
                    result.put("itemCount", 0);
                }
            }

            log.info("API 호출 성공 - totalCount: {}", response.body() != null ? response.body().totalCount() : "N/A");

        } catch (Exception e) {
            log.error("Rent API 호출 실패", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }
}
