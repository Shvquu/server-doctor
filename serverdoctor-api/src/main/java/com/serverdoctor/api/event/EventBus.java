package com.serverdoctor.api.event;

import java.util.function.Consumer;

/** Schlanker, thread-safe Event-Bus für API-Konsumenten. */
public interface EventBus {

    <E extends ServerDoctorEvent> void subscribe(Class<E> type, Consumer<E> subscriber);

    void publish(ServerDoctorEvent event);
}
