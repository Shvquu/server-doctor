package com.serverdoctor.core.event;

import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.api.event.ServerDoctorEvent;
import com.serverdoctor.platform.LoggerAdapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Thread-safe EventBus. Subscriber-Fehler werden isoliert geloggt. */
public final class EventBusImpl implements EventBus {

    private final Map<Class<?>, List<Consumer<?>>> subscribers = new ConcurrentHashMap<>();
    private final LoggerAdapter logger;

    public EventBusImpl(LoggerAdapter logger) { this.logger = logger; }

    @Override
    public <E extends ServerDoctorEvent> void subscribe(Class<E> type, Consumer<E> subscriber) {
        subscribers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(ServerDoctorEvent event) {
        List<Consumer<?>> list = subscribers.get(event.getClass());
        if (list == null) return;
        for (Consumer<?> raw : list) {
            try {
                ((Consumer<ServerDoctorEvent>) raw).accept(event);
            } catch (Exception ex) {
                logger.error("EventBus-Subscriber warf eine Exception für " + event.getClass().getSimpleName(), ex);
            }
        }
    }
}
