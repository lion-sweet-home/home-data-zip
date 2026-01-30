package org.example.homedatazip.global.batch.tradeRent.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.batch.trade-rent")
public class TradeRentBatchProperties {
    private boolean enabled = true;
    private List<Integer> daysOfMonth = List.of(25, 26, 27);
    private int windowMonths = 2;

    public boolean isEnabled() { return enabled; }
    public List<Integer> getDaysOfMonth() { return daysOfMonth; }
    public int getWindowMonths() { return windowMonths; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDaysOfMonth(List<Integer> daysOfMonth) { this.daysOfMonth = daysOfMonth; }
    public void setWindowMonths(int windowMonths) { this.windowMonths = windowMonths; }
}
