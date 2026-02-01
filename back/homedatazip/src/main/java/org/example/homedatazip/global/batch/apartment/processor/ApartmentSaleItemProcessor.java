package org.example.homedatazip.global.batch.apartment.processor;

import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ApartmentSaleItemProcessor implements ItemProcessor<ApartmentTradeSaleItem, ApartmentTradeSaleItem> {

    @Override
    public ApartmentTradeSaleItem process(ApartmentTradeSaleItem item) throws Exception {
        if (item.getDealAmount() == null || item.getDealAmount().isBlank()) {
            return null;
        }

        return item;
    }
}
