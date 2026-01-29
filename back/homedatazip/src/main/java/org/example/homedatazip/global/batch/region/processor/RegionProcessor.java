package org.example.homedatazip.global.batch.region.processor;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.dto.RegionApiResponse;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegionProcessor implements ItemProcessor<RegionApiResponse, RegionApiResponse> {

    @Override
    public RegionApiResponse process(RegionApiResponse response) throws Exception {

        if (response.deletedAt() != null && !response.deletedAt().isBlank()) {
            return null;
        }

        return response;
    }
}
