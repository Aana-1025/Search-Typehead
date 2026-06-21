package com.typeahead.trending;

import java.time.Duration;
import java.time.Instant;

public enum TrendingWindow {
    ONE_HOUR("1h", Duration.ofHours(1)),
    SIX_HOURS("6h", Duration.ofHours(6)),
    TWENTY_FOUR_HOURS("24h", Duration.ofHours(24)),
    SEVEN_DAYS("7d", Duration.ofDays(7));

    private final String apiValue;
    private final Duration duration;

    TrendingWindow(String apiValue, Duration duration) {
        this.apiValue = apiValue;
        this.duration = duration;
    }

    public String apiValue() {
        return apiValue;
    }

    public Instant cutoffFrom(Instant now) {
        return now.minus(duration);
    }

    public static TrendingWindow fromApiValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return TWENTY_FOUR_HOURS;
        }

        String normalizedValue = rawValue.trim().toLowerCase();
        for (TrendingWindow trendingWindow : values()) {
            if (trendingWindow.apiValue.equals(normalizedValue)) {
                return trendingWindow;
            }
        }

        throw new IllegalArgumentException("window must be one of 1h, 6h, 24h, 7d");
    }
}
