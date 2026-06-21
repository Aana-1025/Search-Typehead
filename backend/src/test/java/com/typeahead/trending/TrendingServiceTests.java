package com.typeahead.trending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TrendingServiceTests {

    @Mock
    private TrendingRepository trendingRepository;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-21T16:00:00Z"), ZoneOffset.UTC);

    @Test
    void defaultTrendingRequestUses24hAndLimit10() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);
        when(trendingRepository.findTrendingSince(Instant.parse("2026-06-20T16:00:00Z"))).thenReturn(List.of());

        TrendingResponse response = trendingService.trending(null, null);

        assertThat(response.window()).isEqualTo("24h");
        assertThat(response.count()).isZero();
        verify(trendingRepository).findTrendingSince(Instant.parse("2026-06-20T16:00:00Z"));
    }

    @Test
    void customWindowWorks() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);
        when(trendingRepository.findTrendingSince(Instant.parse("2026-06-21T10:00:00Z"))).thenReturn(List.of());

        TrendingResponse response = trendingService.trending("6h", 10);

        assertThat(response.window()).isEqualTo("6h");
        verify(trendingRepository).findTrendingSince(Instant.parse("2026-06-21T10:00:00Z"));
    }

    @Test
    void customLimitWorks() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);
        when(trendingRepository.findTrendingSince(Instant.parse("2026-06-20T16:00:00Z"))).thenReturn(List.of(
            new TrendingItem("a", 10, 5, 5.0),
            new TrendingItem("b", 9, 4, 4.0),
            new TrendingItem("c", 8, 3, 3.0)
        ));

        TrendingResponse response = trendingService.trending("24h", 2);

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.items()).extracting(TrendingItem::query).containsExactly("a", "b");
    }

    @Test
    void invalidWindowReturns400() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);

        assertThatThrownBy(() -> trendingService.trending("48h", 10))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST")
            .hasMessageContaining("window must be one of 1h, 6h, 24h, 7d");
    }

    @Test
    void invalidLimitReturns400() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);

        assertThatThrownBy(() -> trendingService.trending("24h", 0))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST")
            .hasMessageContaining("limit must be greater than or equal to 1");
    }

    @Test
    void rankingUsesRecentCountFirst() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);
        when(trendingRepository.findTrendingSince(Instant.parse("2026-06-20T16:00:00Z"))).thenReturn(List.of(
            new TrendingItem("zebra", 100, 5, 5.0),
            new TrendingItem("alpha", 150, 5, 5.0),
            new TrendingItem("iphone", 50, 6, 6.0)
        ));

        TrendingResponse response = trendingService.trending("24h", 10);

        assertThat(response.items()).extracting(TrendingItem::query)
            .containsExactly("iphone", "alpha", "zebra");
    }

    @Test
    void serviceReturnsMax10ByDefault() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);
        when(trendingRepository.findTrendingSince(Instant.parse("2026-06-20T16:00:00Z"))).thenReturn(List.of(
            new TrendingItem("q01", 100, 100, 100.0),
            new TrendingItem("q02", 99, 99, 99.0),
            new TrendingItem("q03", 98, 98, 98.0),
            new TrendingItem("q04", 97, 97, 97.0),
            new TrendingItem("q05", 96, 96, 96.0),
            new TrendingItem("q06", 95, 95, 95.0),
            new TrendingItem("q07", 94, 94, 94.0),
            new TrendingItem("q08", 93, 93, 93.0),
            new TrendingItem("q09", 92, 92, 92.0),
            new TrendingItem("q10", 91, 91, 91.0),
            new TrendingItem("q11", 90, 90, 90.0)
        ));

        TrendingResponse response = trendingService.trending(null, null);

        assertThat(response.count()).isEqualTo(10);
        assertThat(response.items()).hasSize(10);
    }

    @Test
    void limitGreaterThan20IsClamped() {
        TrendingService trendingService = new TrendingService(trendingRepository, fixedClock);
        when(trendingRepository.findTrendingSince(Instant.parse("2026-06-20T16:00:00Z"))).thenReturn(List.of(
            new TrendingItem("one", 1, 1, 1.0)
        ));

        TrendingResponse response = trendingService.trending("24h", 25);

        assertThat(response.count()).isEqualTo(1);
    }
}
