package com.serverdoctor.velocity;

import com.google.inject.Inject;
import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.core.messages.MessageStore;
import com.serverdoctor.core.update.UpdateChecker;
import com.serverdoctor.core.update.UpdateResult;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import com.serverdoctor.velocity.platform.VelocityServerPlatform;
import com.serverdoctor.velocity.storage.VelocityStorageSettings;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstiegspunkt auf Velocity. Lädt aus derselben Jar wie das Paper/Folia-Plugin. */
@Plugin(
        id = "serverdoctor",
        name = "ServerDoctor",
        version = "0.7.0",
        description = "Read-only analysis, diagnostics and monitoring for Minecraft networks.",
        authors = {"LittleSophyy", "zNixFNA", "DeltaNimrod"}
)
public final class ServerDoctorVelocityPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private ServerDoctorCore core;
    private StorageProvider storage;
    private MessageStore messages;
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

        this.messages = loadMessages();
        this.storage = openStorage();

        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
            } catch (Exception ex) {
                logger.warn("Persistenz fehlgeschlagen: {}", ex.getMessage());
            }
        });

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("serverdoctor").aliases("sd", "docotor").build(),
                new ServerDoctorVelocityCommand(api, messages, this::reloadMessages));

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

    private MessageStore loadMessages() {
        MessageStore store = new MessageStore();
        try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
            store.loadDefaults(in);
        } catch (Exception ignored) { }
        try {
            Files.createDirectories(dataDirectory);
            Path file = dataDirectory.resolve("messages.yml");
            if (!Files.exists(file)) {
                try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
                    if (in != null) Files.copy(in, file);
                }
            }
            if (Files.exists(file)) {
                store.applyOverrides(Files.readString(file, StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            logger.warn("messages.yml nicht ladbar: {}", ex.getMessage());
        }
        return store;
    }

    /** Lädt messages.yml neu (für /serverdoctor reload). */
    public void reloadMessages() {
        messages.clearOverrides();
        Path file = dataDirectory.resolve("messages.yml");
        if (Files.exists(file)) {
            try { messages.applyOverrides(Files.readString(file, StandardCharsets.UTF_8)); }
            catch (Exception ex) { logger.warn("messages.yml nicht lesbar: {}", ex.getMessage()); }
        }
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
                .orElse("0.7.0");
    }

    private StorageProvider openStorage() {
        StorageConfig cfg;
        try {
            cfg = VelocityStorageSettings.load(dataDirectory, getClass().getResourceAsStream("/config.yml"));
        } catch (Exception ex) {
            logger.warn("Storage-Konfiguration ungültig ({}) - nutze SQLite.", ex.getMessage());
            try {
                java.nio.file.Files.createDirectories(dataDirectory);
            } catch (Exception ignored) { }
            cfg = StorageConfig.sqlite(dataDirectory.resolve("serverdoctor.db").toString());
        }

        try {
            StorageProvider provider = StorageProviders.create(cfg);
            provider.initialize();
            logger.info("Storage: {}", cfg.type());
            return provider;
        } catch (Exception ex) {
            logger.warn("{} nicht verfügbar ({}) - nutze In-Memory-Storage.", cfg.type(), ex.getMessage());
            StorageProvider provider = StorageProviders.create(StorageConfig.memory());
            provider.initialize();
            return provider;
        }
    }
}
