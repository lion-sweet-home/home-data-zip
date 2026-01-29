package org.example.homedatazip.tradeRent.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "molit.rent")
public class MolitRentProperties {
    private String baseUrl;
    private String path;
    private String serviceKey;
    private int numOfRows = 1000;
    private int timeoutSeconds = 15;

    public String getBaseUrl() { return baseUrl; }
    public String getPath() { return path; }
    public String getServiceKey() { return serviceKey; }
    public int getNumOfRows() { return numOfRows; }
    public int getTimeoutSeconds() { return timeoutSeconds; }

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setPath(String path) { this.path = path; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public void setNumOfRows(int numOfRows) { this.numOfRows = numOfRows; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
