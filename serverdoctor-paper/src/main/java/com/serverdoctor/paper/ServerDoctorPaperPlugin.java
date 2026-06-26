package com.serverdoctor.paper;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.PerformanceThresholdReachedEvent;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.advisory.AdvisorySource;
import com.serverdoctor.core.advisory.AdvisorySources;
import com.serverdoctor.core.compat.CompatibilityMetadataSource;
import com.serverdoctor.core.compat.CompatibilityMetadataSources;
import com.serverdoctor.core.config.FilesystemConfigSource;
import com.serverdoctor.core.engine.ScannerSources;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.core.messages.MessageStore;
import com.serverdoctor.core.network.NodeFingerprints;
import com.serverdoctor.core.regression.PerformanceHistory;
import com.serverdoctor.core.update.UpdateChecker;
import com.serverdoctor.core.update.UpdateResult;
import com.serverdoctor.paper.command.ServerDoctorCommand;
import com.serverdoctor.paper.config.PaperRuntimeSettings;
import com.serverdoctor.paper.gui.GuiSettings;
import com.serverdoctor.paper.gui.ServerDoctorGui;
import com.serverdoctor.paper.placeholder.ServerDoctorExpansion;
import com.serverdoctor.paper.platform.PaperServerPlatform;
import com.serverdoctor.paper.service.PaperServiceSettings;
import com.serverdoctor.paper.storage.StorageSettings;
import com.serverdoctor.paper.tasks.TasksSettings;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.rest.RestApiServer;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import com.serverdoctor.storage.repository.NodeRepository;
import com.serverdoctor.webhook.HealthDigest;
import com.serverdoctor.webhook.WebhookConfig;
import com.serverdoctor.webhook.WebhookDispatcher;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** Entry point on Paper/Folia. Wires core, storage, advisory, GUI, tasks, command, REST + webhooks. */
public final class ServerDoctorPaperPlugin extends JavaPlugin {

    private ServerDoctorCore core;
    private StorageProvider storage;
    private MessageStore messageStore;
    private ServerDoctorGui gui;
    private RestApiServer restApi;
    private SchedulerAdapter.Cancellable periodicTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PaperServerPlatform platform = new PaperServerPlatform(this);

        // Advisory source from config (off by default) -> passed into bootstrap.
        AdvisorySource advisories = buildAdvisorySource();
        CompatibilityMetadataSource compat = buildCompatibilitySource();

        this.messageStore = loadMessages();
        this.storage = openStorage();

        PerformanceHistory history = limit -> storage.performance().recent(limit);
        NodeRepository nodeRepository = storage.nodes();
        if (nodeRepository == null) {
            getLogger().warning("storage.nodes() lieferte null (" + storage.getClass().getSimpleName()
                    + ") - Cross-Node bleibt inaktiv. Prüft, ob euer StorageProvider 'nodes' in initialize() zuweist.");
        }
        String nodeName = resolveNodeName(platform);
        WebhookConfig webhookConfig = PaperServiceSettings.webhooks(getConfig());

        ScannerSources sources = ScannerSources.builder()
                .advisory(advisories)
                .compatibility(compat)
                .history(history)
                .config(new FilesystemConfigSource())
                .network(() -> nodeRepository == null ? java.util.List.of() : nodeRepository.others(nodeName))
                .build();

        this.core = ServerDoctorCore.bootstrap(platform, sources);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
                if (nodeRepository != null) {
                    nodeRepository.upsert(NodeFingerprints.of(platform, nodeName));
                }
            } catch (Exception ex) {
                getLogger().warning("Persistenz fehlgeschlagen: " + ex.getMessage());
            }
        });

        // In-game GUI (Paper/Folia)
        GuiSettings guiSettings = PaperRuntimeSettings.gui(getConfig());
        if (guiSettings.enabled()) {
            this.gui = new ServerDoctorGui(this, api, storage, guiSettings);
            gui.register();
        }

        ServerDoctorCommand command = new ServerDoctorCommand(api, storage, messageStore, this::reloadMessages, gui, getDataFolder().toPath(), getServer().getVersion());
        var pluginCommand = getCommand("serverdoctor");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        api.events().subscribe(PerformanceThresholdReachedEvent.class, e ->
                getLogger().warning("[Performance] " + e.reason()));

        // Optional PlaceholderAPI integration - only if installed.
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new ServerDoctorExpansion(this, api).register();
                getLogger().info("PlaceholderAPI-Integration aktiviert.");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI-Hook fehlgeschlagen: " + t.getMessage());
            }
        }

        // REST API (no-op if disabled in config.yml)
        try {
            this.restApi = new RestApiServer(api, PaperServiceSettings.restApi(getConfig()),
                    getDescription().getVersion(), msg -> getLogger().info(msg));
            this.restApi.start();
        } catch (Exception ex) {
            getLogger().warning("REST API konnte nicht starten: " + ex.getMessage());
        }

        // --- Health-Digest ---
        boolean digestOn = getConfig().getBoolean("webhooks.digest.enabled", false);
        long    minutes  = getConfig().getLong("webhooks.digest.interval-minutes", 1440);

        if (webhookConfig.enabled() && digestOn) {
            HealthDigest digest = new HealthDigest(webhookConfig, "", getLogger()::warning);
            long periodTicks = Math.max(1, minutes) * 60L * 20L;
            platform.scheduler().runRepeatingAsync(
                    () -> api.getLatestReport().ifPresent(digest::send),
                    periodTicks, periodTicks);
        }

        // Webhooks (no-op if disabled / no valid targets). Subscribes to the event bus itself.
        new WebhookDispatcher(webhookConfig, api.events(),
                "Paper", msg -> getLogger().warning(msg)).start();

        // Configurable periodic scan (replaces the old hard-coded 5-minute task).
        TasksSettings tasks = PaperRuntimeSettings.tasks(getConfig());
        if (tasks.scanEnabled()) {
            this.periodicTask = platform.scheduler().runRepeatingAsync(() -> {
                var report = api.runDiagnostics();
                if (tasks.warnOnHigh() && report.overallSeverity().atLeast(Severity.HIGH)) {
                    getLogger().warning("ServerDoctor: Status " + report.overallSeverity()
                            + " - /serverdoctor report für Details.");
                }
            }, tasks.initDelayTicks(), tasks.intervalTicks());
        }

        getLogger().info("ServerDoctor aktiviert auf " + platform.serverInfo().version());

        checkForUpdates(platform);
    }

    @Override
    public void onDisable() {
        if (periodicTask != null) periodicTask.cancel();
        if (restApi != null) { restApi.stop(); restApi = null; }
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        ServerDoctorProvider.unregister();
        getLogger().info("ServerDoctor deaktiviert.");
    }

    private AdvisorySource buildAdvisorySource() {
        ConfigurationSection security = getConfig().getConfigurationSection("security");
        ConfigurationSection adv = security == null ? null : security.getConfigurationSection("advisory");
        if (adv == null || !adv.getBoolean("enabled", false)) {
            return AdvisorySources.disabled();
        }
        return AdvisorySources.remote(
                adv.getString("feed-url", ""),
                adv.getLong("refresh-minutes", 360L),
                msg -> getLogger().warning(msg));
    }

    private CompatibilityMetadataSource buildCompatibilitySource() {
        ConfigurationSection compat = getConfig().getConfigurationSection("compatibility");
        ConfigurationSection cm = compat == null ? null : compat.getConfigurationSection("metadata");
        return (cm != null && cm.getBoolean("enabled", false))
                ? CompatibilityMetadataSources.remote(cm.getString("feed-url", ""),
                cm.getLong("refresh-minutes", 1440L),
                msg -> getLogger().warning(msg))
                : CompatibilityMetadataSources.disabled();
    }

    private MessageStore loadMessages() {
        MessageStore store = new MessageStore();
        try (InputStream in = getResource("messages.yml")) {
            store.loadDefaults(in);
        } catch (Exception ignored) { }

        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }
        File file = new File(getDataFolder(), "messages.yml");
        if (file.exists()) {
            try { store.applyOverrides(Files.readString(file.toPath(), StandardCharsets.UTF_8)); }
            catch (Exception ex) { getLogger().warning("messages.yml nicht lesbar: " + ex.getMessage()); }
        }
        return store;
    }

    /** Reloads messages.yml (for /serverdoctor reload). */
    public void reloadMessages() {
        messageStore.clearOverrides();
        File file = new File(getDataFolder(), "messages.yml");
        if (file.exists()) {
            try { messageStore.applyOverrides(Files.readString(file.toPath(), StandardCharsets.UTF_8)); }
            catch (Exception ex) { getLogger().warning("messages.yml nicht lesbar: " + ex.getMessage()); }
        }
    }

    private void checkForUpdates(PaperServerPlatform platform) {
        UpdateChecker updateChecker = new UpdateChecker("Shvquu/server-doctor", getDescription().getVersion());

        platform.scheduler().runAsync(() -> {
            UpdateResult result = updateChecker.check();

            switch (result.status()) {
                case UPDATE_AVAILABLE -> {
                    getLogger().warning("============================================================");
                    getLogger().warning(" Ein Update ist verfügbar: "
                            + result.currentVersion() + " -> " + result.latestVersion());
                    getLogger().warning(" Download: " + result.releaseUrl());
                    getLogger().warning("============================================================");
                    platform.scheduler().runGlobal(() -> {
                        getLogger().warning("ServerDoctor wird deaktiviert, bis das Update eingespielt ist.");
                        getServer().getPluginManager().disablePlugin(this);
                    });
                }
                case UP_TO_DATE   -> getLogger().info("ServerDoctor ist aktuell (" + result.currentVersion() + ").");
                case NO_RELEASES  -> getLogger().info("Update-Prüfung: keine Releases gefunden.");
                case ERROR        -> getLogger().warning("Update-Prüfung fehlgeschlagen: " + result.detail());
            }
        });
    }

    private StorageProvider openStorage() {
        StorageConfig cfg;
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                throw new IllegalStateException("Datenordner konnte nicht erstellt werden.");
            }
            cfg = StorageSettings.from(getConfig(), getDataFolder());
        } catch (Exception ex) {
            getLogger().warning("Storage-Config ungültig (" + ex.getMessage() + ") - nutze SQLite.");
            cfg = StorageConfig.sqlite(new File(getDataFolder(), "serverdoctor.db").getAbsolutePath());
        }
        try {
            StorageProvider provider = StorageProviders.create(cfg);
            provider.initialize();
            getLogger().info("Storage: " + cfg.type());
            return provider;
        } catch (Exception ex) {
            getLogger().warning(cfg.type() + " nicht verfügbar (" + ex.getMessage() + ") - nutze In-Memory-Storage.");
            StorageProvider provider = StorageProviders.create(StorageConfig.memory());
            provider.initialize();
            return provider;
        }
    }

    private String resolveNodeName(PaperServerPlatform platform) {
        String configured = getConfig().getString("network.node-name");
        if (configured != null && !configured.isBlank()) return configured;
        // stable, unique fallback: platform + the server's bind port
        return platform.name().toLowerCase(java.util.Locale.ROOT) + "-" + getServer().getPort();
    }
}