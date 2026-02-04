package org.example.homedatazip.monthAvg.dto;

public enum PeriodOption {
    M6(6),
    Y1(12),
    Y2(24),
    Y3(36);

    private final int months;

    PeriodOption(int months) {
        this.months = months;
    }

    public int months() {
        return months;
    }

    public static PeriodOption fromNullable(String raw) {
        if (raw == null || raw.isBlank()) return M6;
        return PeriodOption.valueOf(raw.trim().toUpperCase());
    }
}
