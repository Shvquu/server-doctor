package com.serverdoctor.bungee;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.OverallSeverityChangedEvent;
import com.serverdoctor.api.event.ScannerFailedEvent;
import com.serverdoctor.bungee.platform.BungeeServerPlatform;
import com.serverdoctor.bungee.service.BungeeServiceSettings;
import com.serverdoctor.bungee.storage.BungeeStorageSettings;
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
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.rest.RestApiServer;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import com.serverdoctor.storage.repository.NodeRepository;
import com.serverdoctor.webhook.HealthDigest;
import com.serverdoctor.webhook.WebhookConfig;
import com.serverdoctor.webhook.WebhookDispatcher;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** Entry point on BungeeCord. Loads from the same jar as the Paper/Folia and Velocity plugins. */
public final class ServerDoctorBungeePlugin extends Plugin {

    private ServerDoctorCore core;
    private StorageProvider storage;
    private MessageStore messages;
    private BungeeServerPlatform platform;
    private Command command;
    private RestApiServer restApi;
    private SchedulerAdapter.Cancellable periodicTask;

    @Override
    public void onEnable() {
        this.platform = new BungeeServerPlatform(getProxy(), getLogger(), this);

        this.messages = loadMessages();
        this.storage = openStorage();                 // also copies config.yml from the jar if missing

        // config.yml now exists -> parse it once for services + advisory
        Map<String, Object> cfg = loadConfig();
        AdvisorySource advisories = buildAdvisorySource(cfg);
        CompatibilityMetadataSource compat = buildCompatibilitySource(cfg);
        NodeRepository nodeRepo = storage.nodes();
        if (nodeRepo == null) {
            getLogger().warning("storage.nodes() returned null (" + storage.getClass().getSimpleName()
                    + ") - cross-node stays inactive. Check that your StorageProvider assigns 'nodes' in initialize().");
        }
        String nodeName = resolveNodeName(cfg);
        PerformanceHistory history = limit -> storage.performance().recent(limit);
        WebhookConfig webhookConfig = BungeeServiceSettings.webhooks(cfg);

        ScannerSources sources = ScannerSources.builder()
                .advisory(advisories)
                .compatibility(compat)
                .history(history)
                .config(new FilesystemConfigSource())
                .network(() -> nodeRepo == null ? java.util.List.of() : nodeRepo.others(nodeName))
                .build();

        this.core = ServerDoctorCore.bootstrap(platform, sources);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
                if (nodeRepo != null) {
                    nodeRepo.upsert(NodeFingerprints.of(platform, nodeName));
                }
            } catch (Exception ex) {
                getLogger().warning("Persistence failed: " + ex.getMessage());
            }
        });

        api.events().subscribe(OverallSeverityChangedEvent.class, e -> {
            if (e.worsened()) {
                getLogger().warning("Status worsened: " + e.previous() + " -> " + e.current()
                        + " (use /serverdoctor report)");
            } else if (e.improved()) {
                getLogger().info("Status improved: " + e.previous() + " -> " + e.current());
            }
        });

        api.events().subscribe(ScannerFailedEvent.class, e -> {
            getLogger().warning("Scanner failed: " + e.error());
        });

        this.command = new ServerDoctorBungeeCommand(api, messages, this::reloadMessages, getDataFolder().toPath(), NodeFingerprints.minecraftVersion(platform.serverInfo().version()));
        getProxy().getPluginManager().registerCommand(this, command);

        // REST API (no-op if disabled in config.yml)
        try {
            this.restApi = new RestApiServer(api, BungeeServiceSettings.restApi(cfg),
                    getDescription().getVersion(), msg -> getLogger().info(msg));
            this.restApi.start();
        } catch (Exception ex) {
            getLogger().warning("REST API could not start: " + ex.getMessage());
        }

        // --- Health-Digest ---
        @SuppressWarnings("unchecked")
        Map<String, Object> digestCfg =
                (cfg.get("webhooks") instanceof Map<?, ?> w && ((Map<String,Object>) w).get("digest") instanceof Map<?, ?> d)
                        ? (Map<String, Object>) d : Map.of();
        boolean digestOn = Boolean.parseBoolean(String.valueOf(digestCfg.getOrDefault("enabled", false)));
        long    minutes  = Long.parseLong(String.valueOf(digestCfg.getOrDefault("interval-minutes", 1440)));

        if (webhookConfig.enabled() && digestOn) {
            HealthDigest digest = new HealthDigest(webhookConfig, "", getLogger()::warning);
            long periodTicks = Math.max(1, minutes) * 60L * 20L;
            platform.scheduler().runRepeatingAsync(
                    () -> api.getLatestReport().ifPresent(digest::send),
                    periodTicks, periodTicks);
        }

        // Webhooks (no-op if disabled / no valid targets). Subscribes to the event bus itself.
        new WebhookDispatcher(webhookConfig, api.events(),
                "BungeeCord", msg -> getLogger().warning(msg)).start();

        this.periodicTask = platform.scheduler().runRepeatingAsync(api::runDiagnostics, 20L * 30L, 20L * 60L * 5L);

        getLogger().info("ServerDoctor enabled on BungeeCord " + getProxy().getVersion());

        checkForUpdate();
        setupMetrics();
    }

    @Override
    public void onDisable() {
        if (periodicTask != null) { periodicTask.cancel(); periodicTask = null; }
        if (restApi != null) { restApi.stop(); restApi = null; }
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        ServerDoctorProvider.unregister();
    }

    private Map<String, Object> loadConfig() {
        try {
            return BungeeServiceSettings.load(getDataFolder());
        } catch (Exception ex) {
            getLogger().warning("config.yml not parseable: " + ex.getMessage());
            return Map.of();
        }
    }

    private AdvisorySource buildAdvisorySource(Map<String, Object> cfg) {
        BungeeServiceSettings.AdvisorySettings a = BungeeServiceSettings.advisory(cfg);
        return a.enabled()
                ? AdvisorySources.remote(a.feedUrl(), a.refreshMinutes(), msg -> getLogger().warning(msg))
                : AdvisorySources.disabled();
    }

    @SuppressWarnings("unchecked")
    private CompatibilityMetadataSource buildCompatibilitySource(Map<String, Object> root) {
        Object c = root.get("compatibility");
        Map<String,Object> compat = c instanceof Map ? (Map<String,Object>) c : Map.of();
        Object m = compat.get("metadata");
        Map<String,Object> md = m instanceof Map ? (Map<String,Object>) m : Map.of();
        boolean enabled = Boolean.parseBoolean(String.valueOf(md.getOrDefault("enabled", false)));
        if (!enabled) return CompatibilityMetadataSources.disabled();
        long refresh;
        try { refresh = Long.parseLong(String.valueOf(md.getOrDefault("refresh-minutes", 1440))); }
        catch (Exception e) { refresh = 1440L; }
        return CompatibilityMetadataSources.remote(String.valueOf(md.getOrDefault("feed-url", "")), refresh, msg -> getLogger().warning(msg));
    }

    private MessageStore loadMessages() {
        MessageStore store = new MessageStore();
        try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
            store.loadDefaults(in);
        } catch (Exception ignored) { }
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            File file = new File(getDataFolder(), "messages.yml");
            if (!file.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
                    if (in != null) Files.copy(in, file.toPath());
                }
            }
            if (file.exists()) {
                store.applyOverrides(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            getLogger().warning("messages.yml not loadable: " + ex.getMessage());
        }
        return store;
    }

    /** Reloads messages.yml (for /serverdoctor reload). */
    public void reloadMessages() {
        messages.clearOverrides();
        File file = new File(getDataFolder(), "messages.yml");
        if (file.exists()) {
            try { messages.applyOverrides(Files.readString(file.toPath(), StandardCharsets.UTF_8)); }
            catch (Exception ex) { getLogger().warning("messages.yml not readable: " + ex.getMessage()); }
        }
    }

    private StorageProvider openStorage() {
        StorageConfig cfg;
        try (InputStream bundled = getClass().getResourceAsStream("/config.yml")) {
            cfg = BungeeStorageSettings.load(getDataFolder(), bundled);
        } catch (Exception ex) {
            getLogger().warning("Storage config invalid (" + ex.getMessage() + ") - using SQLite.");
            cfg = StorageConfig.sqlite(new File(getDataFolder(), "serverdoctor.db").getAbsolutePath());
        }
        try {
            StorageProvider provider = StorageProviders.create(cfg);
            provider.initialize();
            getLogger().info("Storage: " + cfg.type());
            return provider;
        } catch (Exception ex) {
            getLogger().warning(cfg.type() + " unavailable (" + ex.getMessage() + ") - using In-Memory storage.");
            StorageProvider provider = StorageProviders.create(StorageConfig.memory());
            provider.initialize();
            return provider;
        }
    }

    private void checkForUpdate() {
        UpdateChecker updateChecker = new UpdateChecker("Shvquu/server-doctor", getDescription().getVersion());
        platform.scheduler().runAsync(() -> {
            UpdateResult result = updateChecker.check();
            switch (result.status()) {
                case UPDATE_AVAILABLE -> {
                    getLogger().warning("============================================================");
                    getLogger().warning(" An update is available: " + result.currentVersion()
                            + " -> " + result.latestVersion());
                    getLogger().warning(" Download: " + result.releaseUrl());
                    getLogger().warning("============================================================");
                    getLogger().warning("ServerDoctor will go inert until the update is installed.");
                    deactivate();
                }
                case UP_TO_DATE  -> getLogger().info("ServerDoctor is up to date (" + result.currentVersion() + ").");
                case NO_RELEASES -> getLogger().warning("Update check: no releases found.");
                case ERROR       -> getLogger().warning("Update check failed: " + result.detail());
            }
        });
    }

    /** Make the plugin inert: cancel the scan, stop the REST API, unregister the command, close storage. */
    private void deactivate() {
        if (periodicTask != null) { periodicTask.cancel(); periodicTask = null; }
        if (restApi != null) { restApi.stop(); restApi = null; }
        if (command != null) {
            getProxy().getPluginManager().unregisterCommand(command);
            command = null;
        }
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
            storage = null;
        }
        ServerDoctorProvider.unregister();
    }

    @SuppressWarnings("unchecked")
    private String resolveNodeName(Map<String, Object> cfg) {
        Object net = cfg.get("network");
        Map<String, Object> network = net instanceof Map ? (Map<String, Object>) net : Map.of();
        Object raw = network.get("node-name");
        String configured = raw == null ? "" : String.valueOf(raw).trim();
        if (!configured.isEmpty()) return configured;

        // stable fallback: the first listener's query port (unique per BungeeCord instance)
        var listeners = getProxy().getConfig().getListeners();
        int port = listeners.isEmpty() ? 25577 : listeners.iterator().next().getQueryPort();
        return "bungeecord-" + port;
    }

    private void setupMetrics() {
        try {
            new Metrics(this, 32263);
        } catch (Throwable t) {
            getLogger().warning("Failed to setup metrics: " + t.getMessage());
        }
    }
}