package com.typeahead.trending;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrendingService {

    private static final String SOURCE = "postgres";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final Comparator<TrendingItem> TRENDING_ORDER = Comparator
        .comparingLong(TrendingItem::recentCount).reversed()
        .thenComparing(Comparator.comparingLong(TrendingItem::totalCount).reversed())
        .thenComparing(TrendingItem::query);

    private final TrendingRepository trendingRepository;
    private final Clock clock;

    @Autowired
    public TrendingService(TrendingRepository trendingRepository) {
        this(trendingRepository, Clock.systemUTC());
    }

    TrendingService(TrendingRepository trendingRepository, Clock clock) {
        this.trendingRepository = trendingRepository;
        this.clock = clock;
    }

    public TrendingResponse trending(String rawWindow, Integer rawLimit) {
        TrendingWindow trendingWindow = parseWindow(rawWindow);
        int limit = parseLimit(rawLimit);
        Instant cutoff = trendingWindow.cutoffFrom(Instant.now(clock));

        List<TrendingItem> items = trendingRepository.findTrendingSince(cutoff).stream()
            .sorted(TRENDING_ORDER)
            .limit(limit)
            .map(item -> new TrendingItem(
                item.query(),
                item.totalCount(),
                item.recentCount(),
                (double) item.recentCount()
            ))
            .toList();

        return new TrendingResponse(trendingWindow.apiValue(), items.size(), items, SOURCE);
    }

    private TrendingWindow parseWindow(String rawWindow) {
        try {
            return TrendingWindow.fromApiValue(rawWindow);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private int parseLimit(Integer rawLimit) {
        if (rawLimit == null) {
            return DEFAULT_LIMIT;
        }
        if (rawLimit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than or equal to 1");
        }
        return Math.min(rawLimit, MAX_LIMIT);
    }
}
