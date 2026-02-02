package org.example.homedatazip.global.batch.tradeRent.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.service.ApartmentService;
import org.example.homedatazip.tradeRent.dto.ApartmentGetOrCreateRequest;
import org.example.homedatazip.tradeRent.dto.TradeRentWriteRequest;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentBulkRepository;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.batch.item.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeRentWriter implements ItemWriter<TradeRentWriteRequest> {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentService apartmentService;
    private final TradeRentRepository tradeRentRepository;
    private final TradeRentBulkRepository tradeRentBulkRepository;

    private static final boolean USE_UPSERT = false;

    @Override
    @Transactional
    public void write(Chunk<? extends TradeRentWriteRequest> chunk) throws Exception {
        List<? extends TradeRentWriteRequest> items = chunk.getItems();
        if (items == null || items.isEmpty()) return;

        List<ApartmentGetOrCreateRequest> dtos = new ArrayList<>();
        List<TradeRent> tradeRents = new ArrayList<>();

        for(TradeRentWriteRequest item : items){
            ApartmentGetOrCreateRequest dto = item.toApartmentGetOrCreateRequest();
            dtos.add(dto);
        }

        Map<String, Apartment> aptMap = apartmentService.getOrCreateApartmentsFromTradeRent(dtos);

        log.info("아파트 처리 완료 - aptMap 크기: {}", aptMap.size());

        // 3. TradeRent 엔티티 생성
        int skippedCount = 0;
        int skippedAptMissingCount = 0;
        int skippedRequiredMissingCount = 0;
        
        for (TradeRentWriteRequest item : items) {
            Apartment apt = aptMap.get(item.aptSeq());
            if (apt == null) {
                log.debug("아파트 없음 스킵 - aptSeq: {}", item.aptSeq());
                skippedCount++;
                continue;
            }



            Long deposit = item.deposit();
            Integer monthlyRent = item.monthlyRent();
            Double exclusiveArea = item.exclusiveArea();
            Integer floor = item.floor();
            LocalDate dealDate = item.dealDate();
            String sggCd = item.sggCd();
            String rentTerm = normalizeRentTerm(item.contractTerm());

            if (deposit == null || monthlyRent == null || exclusiveArea == null || floor == null || dealDate == null || sggCd == null) {
                skippedRequiredMissingCount++;
                log.warn("필수값 누락 스킵 - aptSeq:{}, deposit:{}, monthlyRent:{}, area:{}, floor:{}, dealDate:{}, sggCd:{}",
                        item.aptSeq(), deposit, monthlyRent, exclusiveArea, floor, dealDate, sggCd);
                continue;
            }


            TradeRent rent = TradeRent.builder()
                    .apartment(apt)
                    .deposit(item.deposit())
                    .monthlyRent(item.monthlyRent())
                    .exclusiveArea(item.exclusiveArea())
                    .floor(item.floor())
                    .dealDate(item.dealDate())
                    .renewalRequested(null)
                    .rentTerm(item.contractTerm() == null ? "-" : item.contractTerm())
                    .sggCd(item.sggCd())
                    .renewalRequested(item.useRRRight())
                    .build();

            tradeRents.add(rent);
        }

        if (tradeRents.isEmpty()) {
            log.info("저장할 TradeRent 없음 - 입력:{}건, 아파트없음:{}건, 필수값누락:{}건",
                    items.size(), skippedAptMissingCount, skippedRequiredMissingCount);
            return;
        }

        if (USE_UPSERT) {
            int[] r = tradeRentBulkRepository.bulkUpsert(tradeRents);
            log.info("TradeRent Upsert 완료 - 입력:{}건, 아파트없음:{}건, 필수값누락:{}건, 저장시도:{}건, 신규:{}건, 업데이트:{}건",
                    items.size(), skippedAptMissingCount, skippedRequiredMissingCount,
                    tradeRents.size(), r[0], r[1]);
        } else {
            int[] r = tradeRentBulkRepository.bulkInsertIgnore(tradeRents);
            log.info("TradeRent InsertIgnore 완료 - 입력:{}건, 아파트없음:{}건, 필수값누락:{}건, 저장시도:{}건, 저장:{}건, DB중복스킵:{}건",
                    items.size(), skippedAptMissingCount, skippedRequiredMissingCount,
                    tradeRents.size(), r[0], r[1]);
        }
    }

    private static String normalizeRentTerm(String contractTerm) {
        if (contractTerm == null) return "-";
        String t = contractTerm.trim();
        return t.isEmpty() ? "-" : t;
        }
    }
