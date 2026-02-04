package org.example.homedatazip.hospital.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * XML 응답 구조
 * <br/>
 * <response>
 *      <header>
 *          <resultCode>...</resultCode>
 *          <resultMsg>...</resultMsg>
 *      </header>
 *      <body>
 *          <items>
 *              <item>...</item>
 *              <item>...</item>
 *          </items>
 *          <numOfRows>...</numOfRows>
 *          <pageNo>...</pageNo>
 *          <totalCount>...</totalCount>
 *      </body>
 * </response>
 */
@Getter
@Setter
@NoArgsConstructor
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HospitalApiResponse {

    @JacksonXmlProperty(localName = "header")
    private Header header;

    @JacksonXmlProperty(localName = "body")
    private Body body;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JacksonXmlElementWrapper(localName = "items")
        @JacksonXmlProperty(localName = "item")
        private List<HospitalItem> items;

        @JacksonXmlProperty(localName = "numOfRows")
        private Integer numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private Integer pageNo;

        @JacksonXmlProperty(localName = "totalCount")
        private Integer totalCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HospitalItem {
        @JacksonXmlProperty(localName = "hpid")
        private String hospitalId;

        @JacksonXmlProperty(localName = "dutyName")
        private String name;

        @JacksonXmlProperty(localName = "dutyDivNam")
        private String typeName;

        @JacksonXmlProperty(localName = "dutyAddr")
        private String address;

        @JacksonXmlProperty(localName = "wgs84Lat")
        private Double latitude;

        @JacksonXmlProperty(localName = "wgs84Lon")
        private Double longitude;
    }

    // 편의 메서드
    public Integer getTotalCount() {
        return body != null ? body.getTotalCount() : null;
    }

    public List<HospitalItem> getItems() {
        return body != null ? body.getItems() : null;
    }

    public boolean isSuccess() {
        return header != null && "00".equals(header.getResultCode());
    }
}
