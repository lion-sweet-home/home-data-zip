package org.example.homedatazip.global.batch.tradeRent.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.batch.trade-rent")
public class TradeRentBatchProperties {
    private boolean enabled ;
    private List<Integer> daysOfMonth;
    private int windowMonths ;

    public boolean isEnabled() { return enabled; }
    public List<Integer> getDaysOfMonth() { return daysOfMonth; }
    public int getWindowMonths() { return windowMonths; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDaysOfMonth(List<Integer> daysOfMonth) { this.daysOfMonth = daysOfMonth; }
    public void setWindowMonths(int windowMonths) { this.windowMonths = windowMonths; }
}
