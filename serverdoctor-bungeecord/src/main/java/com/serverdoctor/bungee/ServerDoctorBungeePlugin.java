package com.serverdoctor.bungee;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.bungee.platform.BungeeServerPlatform;
import com.serverdoctor.bungee.service.BungeeServiceSettings;
import com.serverdoctor.bungee.storage.BungeeStorageSettings;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.core.messages.MessageStore;
import com.serverdoctor.core.update.UpdateChecker;
import com.serverdoctor.core.update.UpdateResult;
import com.serverdoctor.platform.SchedulerAdapter;
import com.serverdoctor.rest.RestApiServer;
import com.serverdoctor.storage.StorageConfig;
import com.serverdoctor.storage.StorageProvider;
import com.serverdoctor.storage.StorageProviders;
import com.serverdoctor.webhook.WebhookDispatcher;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public final class ServerDoctorBungeePlugin extends Plugin {

    private ServerDoctorCore core;
    private StorageProvider storage;
    private MessageStore messages;
    private BungeeServerPlatform platform;
    private Command command;
    private SchedulerAdapter.Cancellable periodicTask;

    private RestApiServer restApiServer;
    private WebhookDispatcher webhooks;

    @Override
    public void onEnable() {
        this.platform = new BungeeServerPlatform(getProxy(), getLogger(), this);
        this.core = ServerDoctorCore.bootstrap(this.platform);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        saveDefaultConfig();

        this.messages = loadMessages();
        this.storage = openStorage();

        api.events().subscribe(AnalysisFinishedEvent.class, e -> {
            try {
                storage.saveReport(e.report());
            } catch (Exception ex) {
                getLogger().warning("Failed to handle analysis finished event \n" + ex.getMessage());
            }
        });

        this.command = new ServerDoctorBungeeCommand(api, messages, this::reloadMessages);
        getProxy().getPluginManager().registerCommand(this, command);

        this.periodicTask = platform.scheduler().runRepeatingAsync(api::runDiagnostics, 20L * 30L, 20L * 60L * 5L);

        getLogger().info("ServerDoctorBungeePlugin has been enabled");

        try {
            this.restApiServer = new RestApiServer(api, BungeeServiceSettings.restApi(getConfig()),
                    getDescription().getVersion(), msg -> getLogger().info(msg));
            restApiServer.start();
        } catch (Exception e) {
            getLogger().warning("Rest-API-Server konnte nicht gestartet werden: " + e.getMessage());
        }
        try {
            this.webhooks = new WebhookDispatcher(BungeeServiceSettings.webhooks(getConfig()),
                    api.events(), "BungeeCord " + platform.serverInfo().version(), msg -> getLogger().warning(msg));
            webhooks.start();
            getLogger().info("Webhook-Server aktiviert");
        } catch (Exception ex) {
            getLogger().warning("Webhook-Server konnte nicht gestartet werden: " + ex.getMessage());
        }

        checkForUpdate();
    }

    @Override
    public void onDisable() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
        if (storage != null) {
            try { storage.close(); } catch (Exception ex) { getLogger().warning("Failed to close storage \n" + ex.getMessage()); }
        }
        ServerDoctorProvider.unregister();

        if (restApiServer != null) restApiServer.stop();
        getLogger().info("Plugin disabled");
    }

    private MessageStore loadMessages() {
        MessageStore store = new MessageStore();
        try (InputStream in = getResourceAsStream("/messages.yml")) {
            store.loadDefaults(in);
        } catch (Exception ignored) { }
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            File file = new File(getDataFolder(), "messages.yml");
            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("/messages.yml")) {
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
        try (InputStream bundled = getResourceAsStream("/config.yml")) {
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
                case NO_RELEASES -> getLogger().info("Update check: no releases found.");
                case ERROR       -> getLogger().warning("Update check failed: " + result.detail());
            }
        });
    }

    private Configuration getConfig() {
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException exception) {
            getLogger().warning("Failed to load config.yml: " + exception.getMessage());
            return null;
        }
    }

    /** Make the plugin inert: cancel the scan, unregister the command, close storage. */
    private void deactivate() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
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

    private void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File file = new File(getDataFolder(), "config.yml");
        if (file .exists()) return;

        try (InputStream in = getResourceAsStream("config.yml");
             OutputStream out = new FileOutputStream(file)) {
            if (in == null) {
                throw new IllegalStateException("config.yml not found");
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save config.yml: " + ex.getMessage());
        }
    }
}
