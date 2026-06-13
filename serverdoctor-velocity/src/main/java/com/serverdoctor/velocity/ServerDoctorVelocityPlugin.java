package com.serverdoctor.velocity;

import com.google.inject.Inject;
import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.core.update.UpdateChecker;
import com.serverdoctor.core.update.UpdateResult;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import com.serverdoctor.velocity.platform.VelocityServerPlatform;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/** Einstiegspunkt auf Velocity. Lädt aus derselben Jar wie das Paper/Folia-Plugin. */
@Plugin(
        id = "serverdoctor",
        name = "ServerDoctor",
        version = "0.6.0",
        description = "Read-only analysis, diagnostics and monitoring for Minecraft networks.",
        authors = {"LittleSophyy", "zNixFNA", "DeltaNimrod"}
)
public final class ServerDoctorVelocityPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private ServerDoctorCore core;
    private StorageProvider storage;
    private SchedulerAdapter.Cancellable periodicTask;

    @Inject
    public ServerDoctorVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        VelocityServerPlatform platform = new VelocityServerPlatform(proxy, logger, this);
        this.core = ServerDoctorCore.bootstrap(platform);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        this.storage = openStorage();
        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
            } catch (Exception ex) {
                logger.warn("Persistenz fehlgeschlagen: {}", ex.getMessage());
            }
        });

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("serverdoctor").aliases("sd").build(),
                new ServerDoctorVelocityCommand(api));

        platform.scheduler().runRepeatingAsync(api::runDiagnostics, 20L * 30L, 20L * 60L * 5L);

        logger.info("ServerDoctor aktiviert auf Velocity {}", proxy.getVersion().getVersion());

        checkForUpdate(platform);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        ServerDoctorProvider.unregister();
    }

    private void checkForUpdate(VelocityServerPlatform platform) {
        UpdateChecker updateChecker = new UpdateChecker("Shvquu/server-doctor", currentVersion());
        platform.scheduler().runAsync(() -> {
            UpdateResult result = updateChecker.check();
            switch (result.status()) {
                case UPDATE_AVAILABLE -> {
                    logger.warn("============================================================");
                    logger.warn(" Ein Update ist verfügbar: {} -> {}",
                            result.currentVersion(), result.latestVersion());
                    logger.warn(" Download: {}", result.releaseUrl());
                    logger.warn("============================================================");
                    logger.warn("ServerDoctor wird inert geschaltet, bis das Update eingespielt ist.");
                    deactivate();
                }
                case UP_TO_DATE  -> logger.info("ServerDoctor ist aktuell ({}).", result.currentVersion());
                case NO_RELEASES -> logger.info("Update-Prüfung: keine Releases gefunden.");
                case ERROR       -> logger.warn("Update-Prüfung fehlgeschlagen: {}", result.detail());
            }
        });
    }

    /**
     * Velocity bietet keine API, mit der ein Plugin sich selbst entlädt. Stattdessen
     * machen wir das Plugin funktionslos: Befehl abmelden, Scan stoppen, Storage schließen.
     */
    private void deactivate() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
        proxy.getCommandManager().unregister("serverdoctor");
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
            storage = null;
        }
        ServerDoctorProvider.unregister();
    }

    /** Liest die eigene Version aus dem Plugin Container (Fallback: 0.6.0). **/
    private String currentVersion() {
        return proxy.getPluginManager().fromInstance(this)
                .flatMap(container -> container.getDescription().getVersion())
                .orElse("0.6.0");
    }


    private StorageProvider openStorage() {
        try {
            Files.createDirectories(dataDirectory);
            Path db = dataDirectory.resolve("serverdoctor.db");
            StorageProvider provider = StorageProviders.create(StorageConfig.sqlite(db.toString()));
            provider.initialize();
            logger.info("Storage: SQLite ({})", db);
            return provider;
        } catch (Exception ex) {
            logger.warn("SQLite nicht verfügbar ({}) - nutze In-Memory-Storage.", ex.getMessage());
            StorageProvider provider = StorageProviders.create(StorageConfig.memory());
            provider.initialize();
            return provider;
        }
    }
}
