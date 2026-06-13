package com.serverdoctor.paper;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.PerformanceThresholdReachedEvent;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.engine.ServerDoctorCore;
import com.serverdoctor.paper.command.ServerDoctorCommand;
import com.serverdoctor.paper.platform.PaperServerPlatform;
import com.serverdoctor.platform.SchedulerAdapter;
import org.bukkit.plugin.java.JavaPlugin;

/** Einstiegspunkt auf Paper. Verdrahtet Core, Command und einen periodischen Scan. */
public final class ServerDoctorPaperPlugin extends JavaPlugin {

    private ServerDoctorCore core;
    private SchedulerAdapter.Cancellable periodicTask;

    @Override
    public void onEnable() {
        PaperServerPlatform platform = new PaperServerPlatform(this);
        this.core = ServerDoctorCore.bootstrap(platform);
        ServerDoctorApi api = core.api();
        ServerDoctorProvider.register(api);

        var command = new ServerDoctorCommand(api);
        var pluginCommand = getCommand("serverdoctor");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // Beispiel-Eventnutzung: Performance-Warnungen ins Log.
        api.events().subscribe(PerformanceThresholdReachedEvent.class, e ->
                getLogger().warning("[Performance] " + e.reason()));

        // Periodischer Hintergrund-Scan (alle 5 Minuten), asynchron - Folia-safe via Adapter.
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

    @Override
    public void onDisable() {
        if (periodicTask != null) periodicTask.cancel();
        ServerDoctorProvider.unregister();
        getLogger().info("ServerDoctor deaktiviert.");
    }
}
