package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    /** 위/경도가 있는 아파트만 조회 (아파트–지하철 거리 배치용) */
    List<Apartment> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    List<Apartment> findAllByAptSeqIn(Collection<String> aptSeqs);

    // [추가] 단건 조회를 위한 메서드 (중복 에러 방어용)
    Optional<Apartment> findByAptSeq(String aptSeq);

    // [추가] 존재 여부만 빠르게 확인하기 위한 메서드
    boolean existsByAptSeq(String aptSeq);


    //조회성능을 위한 시구동을 통한 region fetch Join + 아파트 Get + null체크
    //deposit || MonthlyRent 필터선택 후 조회시 추가 필터
    @Query("""
        select a
        from Apartment a
        where a.region.sido = :sido
          and a.region.gugun = :gugun
          and a.region.dong like concat(:dongPrefix, '%')
          and exists (
              select 1
              from TradeRent tr
              where tr.apartment = a
                and (:minDeposit = 0 or tr.deposit >= :minDeposit)
                and (:maxDeposit = 0 or tr.deposit <= :maxDeposit)
                and (:minMonthlyRent = 0 or tr.monthlyRent >= :minMonthlyRent)
                and (:maxMonthlyRent = 0 or tr.monthlyRent <= :maxMonthlyRent)
          )
        """)
    List<Apartment> findAllWithRentByRegionAndRentRange(
            @Param("sido") String sido,
            @Param("gugun") String gugun,
            @Param("dongPrefix") String dongPrefix,
            @Param("minDeposit") long minDeposit,
            @Param("maxDeposit") long maxDeposit,
            @Param("minMonthlyRent") int minMonthlyRent,
            @Param("maxMonthlyRent") int maxMonthlyRent
    );

    @Modifying
    @Transactional
    @Query(value = """
    INSERT IGNORE INTO apartments 
    (apt_name, road_address, jibun_address, latitude, longitude, build_year, apt_seq, region_id) 
    VALUES (:name, :road, :jibun, :lat, :lon, :year, :seq, :regionId)
    """, nativeQuery = true)
    void insertIgnore(@Param("name") String name,
                      @Param("road") String road,
                      @Param("jibun") String jibun,
                      @Param("lat") Double lat,
                      @Param("lon") Double lon,
                      @Param("year") Integer year,
                      @Param("seq") String seq,
                      @Param("regionId") Long regionId);

    List<Apartment> findByRegionIdOrderByAptNameAsc(Long regionId);
}
