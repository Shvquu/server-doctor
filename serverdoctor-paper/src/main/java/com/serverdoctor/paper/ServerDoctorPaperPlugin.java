package com.serverdoctor.paper;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.PerformanceThresholdReachedEvent;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.paper.command.ServerDoctorCommand;
import com.serverdoctor.paper.platform.PaperServerPlatform;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Einstiegspunkt auf Paper. Verdrahtet Core, Storage, Command und periodischen Scan. */
public final class ServerDoctorPaperPlugin extends JavaPlugin {

    private ServerDoctorCore core;
    private StorageProvider storage;
    private SchedulerAdapter.Cancellable periodicTask;

    @Override
    public void onEnable() {
        PaperServerPlatform platform = new PaperServerPlatform(this);
        this.core = ServerDoctorCore.bootstrap(platform);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        this.storage = openStorage();
        // Jeder abgeschlossene Lauf wird persistiert (entkoppelt über den EventBus).
        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
            } catch (Exception ex) {
                getLogger().warning("Persistenz fehlgeschlagen: " + ex.getMessage());
            }
        });

        ServerDoctorCommand command = new ServerDoctorCommand(api, storage);
        var pluginCommand = getCommand("serverdoctor");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        api.events().subscribe(PerformanceThresholdReachedEvent.class, e ->
                getLogger().warning("[Performance] " + e.reason()));

        long fiveMinutes = 20L * 60L * 5L;
        this.periodicTask = platform.scheduler().runRepeatingAsync(() -> {
            var report = api.runDiagnostics();
            if (report.overallSeverity().atLeast(Severity.HIGH)) {
                getLogger().warning("ServerDoctor: Status " + report.overallSeverity()
                        + " - /serverdoctor report für Details.");
            }
        }, 20L * 30L, fiveMinutes);

        getLogger().info("ServerDoctor aktiviert auf " + platform.serverInfo().version());
    }

    private StorageProvider openStorage() {
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                throw new IllegalStateException("Datenordner konnte nicht erstellt werden.");
            }
            File db = new File(getDataFolder(), "serverdoctor.db");
            StorageProvider provider = StorageProviders.create(StorageConfig.sqlite(db.getAbsolutePath()));
            provider.initialize();
            getLogger().info("Storage: SQLite (" + db.getName() + ")");
            return provider;
        } catch (Exception ex) {
            getLogger().warning("SQLite nicht verfügbar (" + ex.getMessage() + ") - nutze In-Memory-Storage.");
            StorageProvider provider = StorageProviders.create(StorageConfig.memory());
            provider.initialize();
            return provider;
        }
    }

    @Override
    public void onDisable() {
        if (periodicTask != null) periodicTask.cancel();
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        ServerDoctorProvider.unregister();
        getLogger().info("ServerDoctor deaktiviert.");
    }
}
