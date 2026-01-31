package org.example.homedatazip.global.batch.tradeRent.wrtier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.service.ApartmentService;
import org.example.homedatazip.tradeRent.dto.ApartmentGetOrCreateRequest;
import org.example.homedatazip.tradeRent.dto.TradeRentWriteRequest;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.batch.item.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeRentWriter implements ItemWriter<TradeRentWriteRequest> {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentService apartmentService;
    private final TradeRentRepository tradeRentRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends TradeRentWriteRequest> chunk) throws Exception {
        List<? extends TradeRentWriteRequest> items = chunk.getItems();
        if (items == null || items.isEmpty()) return;

        List<TradeRent> toSave = new ArrayList<>(items.size());

        for(TradeRentWriteRequest item : items){
            ApartmentGetOrCreateRequest dto = item.toApartmentGetOrCreateRequest();
//            Apartment apt = apartmentRepository.findOrCreateApartment(dto);

            TradeRent rent = TradeRent.builder()
//                    .apartment(apt)
                    .deposit(item.deposit())
                    .monthlyRent(item.monthlyRent())
                    .exclusiveArea(item.exclusiveArea())
                    .floor(item.floor())
                    .dealDate(item.dealDate())
                    .renewalRequested(null)
                    .rentTerm(item.contractTerm() == null ? "-" : item.contractTerm())
                    .sggCd(item.sggCd())
                    .build();

            toSave.add(rent);
        }
        if(toSave.isEmpty()) return;

        try{
            tradeRentRepository.saveAll(toSave);
            tradeRentRepository.flush();
        }catch(DataIntegrityViolationException bulkFail){
            for(TradeRent item : toSave){
                try{
                    tradeRentRepository.save(item);
                }catch(DataIntegrityViolationException ignore){
                    log.info(ignore.getMessage());
                }
            }
            tradeRentRepository.flush();
        }
    }
}
