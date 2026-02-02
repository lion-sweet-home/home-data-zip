package org.example.homedatazip.tradeRent.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeRentFindService {

    private final TradeRentRepository tradeRentRepository;

    @Transactional(readOnly = true)
    public List<TradeRent> getListRentsByAptId(long aptId){

        return tradeRentRepository.findByApartmentId(aptId);
    }


}
