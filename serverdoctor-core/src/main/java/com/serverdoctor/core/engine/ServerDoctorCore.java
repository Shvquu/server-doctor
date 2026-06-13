package com.serverdoctor.core.engine;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.event.EventBus;
import com.serverdoctor.core.conflict.ConflictDatabase;
import com.serverdoctor.core.event.EventBusImpl;
import com.serverdoctor.core.recommendation.RecommendationEngine;
import com.serverdoctor.core.scanner.ConflictScanner;
import com.serverdoctor.core.scanner.DependencyScanner;
import com.serverdoctor.core.scanner.PerformanceScanner;
import com.serverdoctor.core.scanner.PluginScanner;
import com.serverdoctor.core.scanner.SecurityScanner;
import com.serverdoctor.platform.ServerPlatform;

/**
 * Plattformunabhängiger Bootstrap: verdrahtet Registry, Default-Scanner,
 * Recommendation-Engine, EventBus, Engine und liefert die fertige Public-API.
 */
public final class ServerDoctorCore {

    private final ServerDoctorApiImpl api;
    private final EventBus eventBus;
    private final ScannerRegistry registry;
    private final ConflictDatabase conflictDatabase;

    private ServerDoctorCore(ServerDoctorApiImpl api, EventBus bus,
                             ScannerRegistry registry, ConflictDatabase db) {
        this.api = api;
        this.eventBus = bus;
        this.registry = registry;
        this.conflictDatabase = db;
    }

    public static ServerDoctorCore bootstrap(ServerPlatform platform) {
        EventBus eventBus = new EventBusImpl(platform.logger());
        ScannerRegistry registry = new ScannerRegistry();
        ConflictDatabase conflictDatabase = ConflictDatabase.withDefaults();

        registry.register(new PluginScanner());
        registry.register(new DependencyScanner());
        registry.register(new ConflictScanner(conflictDatabase));
        registry.register(new PerformanceScanner());
        registry.register(new SecurityScanner());

        RecommendationEngine recommendations = new RecommendationEngine();
        AnalysisEngine engine = new AnalysisEngine(platform, registry, recommendations, eventBus);
        ServerDoctorApiImpl api = new ServerDoctorApiImpl(engine, registry, eventBus);

        return new ServerDoctorCore(api, eventBus, registry, conflictDatabase);
    }

    public ServerDoctorApi api() { return api; }
    public EventBus eventBus() { return eventBus; }
    public ScannerRegistry registry() { return registry; }
    public ConflictDatabase conflictDatabase() { return conflictDatabase; }
}
