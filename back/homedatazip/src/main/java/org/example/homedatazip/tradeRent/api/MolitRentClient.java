package org.example.homedatazip.tradeRent.api;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.example.homedatazip.apartment.dto.ApiResponse;
import org.example.homedatazip.tradeRent.dto.MolitRentApiItemResponse;
import org.example.homedatazip.tradeRent.dto.MolitRentApiResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class MolitRentClient {

    private final MolitRentProperties props;
    private final XmlMapper xmlMapper;
    private final WebClient webClient;

    public MolitRentClient(MolitRentProperties props, XmlMapper xmlMapper) {
        this.props = props;
        this.xmlMapper = xmlMapper;
        this.webClient = WebClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    public ApiResponse<MolitRentApiItemResponse> fetch(String sggCd5, String dealYmd6, int page){


        String uri = UriComponentsBuilder.fromPath(props.getBaseUrl())
                .queryParam("LAWD_CD", sggCd5)
                .queryParam("DEAL_YMD", dealYmd6)
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("pageNo", page)
                .queryParam("numOfRows", props.getNumOfRows())
                .build(false)
                .toUriString();

        String xml = webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .onErrorResume(e -> Mono.empty())
                .block();

        if(xml == null || xml.isBlank()) return  null;

        try{
            ApiResponse<MolitRentApiItemResponse> res =
                    xmlMapper.readValue(xml, new TypeReference<ApiResponse<MolitRentApiItemResponse>>() {});
            return res;
        }catch (Exception e){
            return null;
        }
    }
}
