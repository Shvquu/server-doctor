package com.serverdoctor.paper;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.PerformanceThresholdReachedEvent;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.core.messages.MessageStore;
import com.serverdoctor.core.update.UpdateChecker;
import com.serverdoctor.core.update.UpdateResult;
import com.serverdoctor.paper.command.ServerDoctorCommand;
import com.serverdoctor.paper.placeholder.ServerDoctorExpansion;
import com.serverdoctor.paper.platform.PaperServerPlatform;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Einstiegspunkt auf Paper. Verdrahtet Core, Storage, Command und periodischen Scan. */
public final class ServerDoctorPaperPlugin extends JavaPlugin {

    private ServerDoctorCore core;
    private StorageProvider storage;
    private MessageStore messageStore;
    private SchedulerAdapter.Cancellable periodicTask;

    @Override
    public void onEnable() {
        PaperServerPlatform platform = new PaperServerPlatform(this);
        this.core = ServerDoctorCore.bootstrap(platform);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        this.messageStore = loadMessages();
        this.storage = openStorage();
        // Jeder abgeschlossene Lauf wird persistiert (entkoppelt über den EventBus).
        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
            } catch (Exception ex) {
                getLogger().warning("Persistenz fehlgeschlagen: " + ex.getMessage());
            }
        });

        ServerDoctorCommand command = new ServerDoctorCommand(api, storage, messageStore, this::reloadMessages);
        var pluginCommand = getCommand("serverdoctor");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        api.events().subscribe(PerformanceThresholdReachedEvent.class, e ->
                getLogger().warning("[Performance] " + e.reason()));

        // Optionale PlaceholderAPI-Integration - nur wenn installiert.
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new ServerDoctorExpansion(this, api).register();
                getLogger().info("PlaceholderAPI-Integration aktiviert.");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI-Hook fehlgeschlagen: " + t.getMessage());
            }
        }

        long fiveMinutes = 20L * 60L * 5L;
        this.periodicTask = platform.scheduler().runRepeatingAsync(() -> {
            var report = api.runDiagnostics();
            if (report.overallSeverity().atLeast(Severity.HIGH)) {
                getLogger().warning("ServerDoctor: Status " + report.overallSeverity()
                        + " - /serverdoctor report für Details.");
            }
        }, 20L * 30L, fiveMinutes);

        getLogger().info("ServerDoctor aktiviert auf " + platform.serverInfo().version());

        checkForUpdates(platform);
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

    /** Lädt messages.yml neu (für /serverdoctor reload). */
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
