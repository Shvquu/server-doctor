package com.serverdoctor.velocity;

import com.google.inject.Inject;
import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.core.advisory.AdvisorySource;
import com.serverdoctor.core.advisory.AdvisorySources;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.core.messages.MessageStore;
import com.serverdoctor.core.update.UpdateChecker;
import com.serverdoctor.core.update.UpdateResult;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.rest.RestApiServer;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import com.serverdoctor.velocity.platform.VelocityServerPlatform;
import com.serverdoctor.velocity.service.VelocityServiceSettings;
import com.serverdoctor.velocity.storage.VelocityStorageSettings;
import com.serverdoctor.webhook.WebhookDispatcher;
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
import java.util.Map;

/** Entry point on Velocity. Loads from the same jar as the Paper/Folia and BungeeCord plugins. */
@Plugin(
        id = "serverdoctor",
        name = "ServerDoctor",
        version = "0.9.0",
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
    private RestApiServer restApi;
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

        this.messages = loadMessages();
        this.storage = openStorage();                 // also copies config.yml from the jar if missing

        // config.yml now exists -> parse once for services + advisory
        Map<String, Object> cfg = loadConfig();
        AdvisorySource advisories = advisoryFrom(cfg);

        this.core = ServerDoctorCore.bootstrap(platform, advisories);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
            } catch (Exception ex) {
                logger.warn("Persistenz fehlgeschlagen: {}", ex.getMessage());
            }
        });

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("serverdoctor").aliases("sd").build(),
                new ServerDoctorVelocityCommand(api, messages, this::reloadMessages));

        // REST API (no-op if disabled in config.yml)
        try {
            this.restApi = new RestApiServer(api, VelocityServiceSettings.restApi(cfg),
                    currentVersion(), logger::info);
            this.restApi.start();
        } catch (Exception ex) {
            logger.warn("REST API konnte nicht starten: {}", ex.getMessage());
        }

        // Webhooks (no-op if disabled / no valid targets). Subscribes to the event bus itself.
        new WebhookDispatcher(VelocityServiceSettings.webhooks(cfg), api.events(),
                "Velocity", logger::warn).start();

        this.periodicTask = platform.scheduler().runRepeatingAsync(api::runDiagnostics, 20L * 30L, 20L * 60L * 5L);

        logger.info("ServerDoctor aktiviert auf Velocity {}", proxy.getVersion().getVersion());

        checkForUpdate(platform);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (periodicTask != null) { periodicTask.cancel(); periodicTask = null; }
        if (restApi != null) { restApi.stop(); restApi = null; }
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        ServerDoctorProvider.unregister();
    }

    private Map<String, Object> loadConfig() {
        try {
            return VelocityServiceSettings.load(dataDirectory);
        } catch (Exception ex) {
            logger.warn("config.yml nicht lesbar: {}", ex.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private AdvisorySource advisoryFrom(Map<String, Object> root) {
        Object sec = root.get("security");
        Map<String, Object> s = sec instanceof Map ? (Map<String, Object>) sec : Map.of();
        Object a = s.get("advisory");
        Map<String, Object> adv = a instanceof Map ? (Map<String, Object>) a : Map.of();
        boolean enabled = Boolean.parseBoolean(String.valueOf(adv.getOrDefault("enabled", false)));
        if (!enabled) return AdvisorySources.disabled();
        long refresh;
        try { refresh = Long.parseLong(String.valueOf(adv.getOrDefault("refresh-minutes", 360))); }
        catch (Exception e) { refresh = 360L; }
        return AdvisorySources.remote(String.valueOf(adv.getOrDefault("feed-url", "")), refresh, logger::warn);
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

    /** Reloads messages.yml (for /serverdoctor reload). */
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

    /** Velocity offers no self-unload API; instead we make the plugin inert. */
    private void deactivate() {
        if (periodicTask != null) { periodicTask.cancel(); periodicTask = null; }
        if (restApi != null) { restApi.stop(); restApi = null; }
        proxy.getCommandManager().unregister("serverdoctor");
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
            storage = null;
        }
        ServerDoctorProvider.unregister();
    }

    private String currentVersion() {
        return proxy.getPluginManager().fromInstance(this)
                .flatMap(container -> container.getDescription().getVersion())
                .orElse("0.9.0");
    }

    private StorageProvider openStorage() {
        StorageConfig cfg;
        try (InputStream bundled = getClass().getResourceAsStream("/config.yml")) {
            cfg = VelocityStorageSettings.load(dataDirectory, bundled);
        } catch (Exception ex) {
            logger.warn("Storage-Config ungültig ({}) - nutze SQLite.", ex.getMessage());
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
