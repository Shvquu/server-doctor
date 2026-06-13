package com.serverdoctor.core.event;

import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.platform.LoggerAdapter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusImplTest {

    private static final LoggerAdapter SILENT = new LoggerAdapter() {
        public void info(String m) {}
        public void warn(String m) {}
        public void error(String m, Throwable t) {}
    };

    @Test void deliversToSubscribers() {
        EventBusImpl bus = new EventBusImpl(SILENT);
        AtomicInteger count = new AtomicInteger();
        bus.subscribe(AnalysisFinishedEvent.class, e -> count.incrementAndGet());
        bus.publish(new AnalysisFinishedEvent(null));
        assertEquals(1, count.get());
    }

    @Test void faultySubscriberDoesNotBreakOthers() {
        EventBusImpl bus = new EventBusImpl(SILENT);
        AtomicInteger reached = new AtomicInteger();
        bus.subscribe(AnalysisFinishedEvent.class, e -> { throw new RuntimeException("boom"); });
        bus.subscribe(AnalysisFinishedEvent.class, e -> reached.incrementAndGet());
        assertDoesNotThrow(() -> bus.publish(new AnalysisFinishedEvent(null)));
        assertEquals(1, reached.get(), "Zweiter Subscriber muss trotz Fehler im ersten laufen");
    }
}
