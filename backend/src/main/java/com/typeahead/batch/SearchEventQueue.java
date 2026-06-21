package com.typeahead.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class SearchEventQueue {

    private final Queue<SearchEvent> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger();

    public int enqueue(SearchEvent searchEvent) {
        queue.add(searchEvent);
        return size.incrementAndGet();
    }

    public void requeueAll(Collection<SearchEvent> searchEvents) {
        for (SearchEvent searchEvent : searchEvents) {
            queue.add(searchEvent);
            size.incrementAndGet();
        }
    }

    public List<SearchEvent> drain(int maxDrainEvents) {
        int drainLimit = Math.max(0, maxDrainEvents);
        List<SearchEvent> drainedEvents = new ArrayList<>(drainLimit);
        for (int index = 0; index < drainLimit; index++) {
            SearchEvent searchEvent = queue.poll();
            if (searchEvent == null) {
                break;
            }
            drainedEvents.add(searchEvent);
            size.decrementAndGet();
        }
        return drainedEvents;
    }

    public int size() {
        return Math.max(0, size.get());
    }
}
