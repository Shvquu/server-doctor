package com.serverdoctor.example;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.AnalysisFinishedEvent;
import com.serverdoctor.api.event.OverallSeverityChangedEvent;
import com.serverdoctor.api.event.PerformanceThresholdReachedEvent;
import com.serverdoctor.api.event.PluginConflictDetectedEvent;
import com.serverdoctor.api.event.RecommendationGeneratedEvent;
import com.serverdoctor.api.event.ScannerFailedEvent;
import com.serverdoctor.api.event.SecurityRiskDetectedEvent;
import com.serverdoctor.api.exception.ApiNotInitializedException;
import com.serverdoctor.common.model.Recommendation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reference integration for the ServerDoctor API.
 *
 * <p>Shows the three things a consumer typically does:
 * <ol>
 *   <li>obtain the API via {@link ServerDoctorProvider#get()},</li>
 *   <li>read live diagnostics (server info, performance, conflicts, recommendations),</li>
 *   <li>subscribe to events and register a custom scanner.</li>
 * </ol>
 *
 * It never writes anything to the server — it only observes ServerDoctor.
 */
public final class ServerDoctorExamplePlugin extends JavaPlugin {

    private ServerDoctorApi api;
    private final ExampleScanner scanner = new ExampleScanner();

    @Override
    public void onEnable() {
        try {
            // 'depend: [ServerDoctor]' guarantees the API is registered before we enable.
            this.api = ServerDoctorProvider.get();
        } catch (ApiNotInitializedException ex) {
            getLogger().severe("ServerDoctor API is not available - is the ServerDoctor plugin installed?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 1) Read basic server facts exposed by the API (new in 1.0.0).
        getLogger().info("Connected to ServerDoctor.");
        getLogger().info("  Server : " + api.getServerInfo().platform()
                + " " + api.getServerInfo().version()
                + " (Java " + api.getServerInfo().javaVersion() + ")");
        getLogger().info("  Plugins: " + api.getPlugins().size());
        getLogger().info("  Scanners: " + String.join(", ", api.getRegisteredModuleIds()));

        // 2) Register our own scanner so it contributes findings to every run.
        api.registerModule(scanner);

        // 3) Subscribe to the events we care about.
        api.events().subscribe(AnalysisFinishedEvent.class, e ->
                getLogger().info("Analysis finished - overall severity: "
                        + e.report().overallSeverity()));

        api.events().subscribe(OverallSeverityChangedEvent.class, e -> {
            if (e.worsened()) {
                getLogger().warning("Status worsened: " + e.previous() + " -> " + e.current());
            } else if (e.improved()) {
                getLogger().info("Status improved: " + e.previous() + " -> " + e.current());
            }
        });

        api.events().subscribe(ScannerFailedEvent.class, e ->
                getLogger().warning("Scanner '" + e.moduleId() + "' failed: " + e.error()));

        api.events().subscribe(PluginConflictDetectedEvent.class, e ->
                getLogger().warning("Conflict: " + e.conflict().description()));

        api.events().subscribe(SecurityRiskDetectedEvent.class, e ->
                getLogger().warning("Security risk in " + e.risk().pluginName()
                        + ": " + e.risk().description()));

        api.events().subscribe(RecommendationGeneratedEvent.class, e ->
                getLogger().info("Recommendation: " + e.recommendation().title()));

        // Only ever fires on Paper/Folia (proxies have no tick loop), but it's safe to subscribe.
        api.events().subscribe(PerformanceThresholdReachedEvent.class, e ->
                getLogger().warning("[Performance] " + e.severity() + " - " + e.reason()));

        getLogger().info("ServerDoctorExample ready - try /sdexample");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            api.unregisterModule(scanner.id());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (api == null) {
            sender.sendMessage("ServerDoctor API not available.");
            return true;
        }
        sender.sendMessage("== ServerDoctor summary ==");
        sender.sendMessage("Overall severity: " + api.getOverallSeverity());
        sender.sendMessage("Last run: "
                + api.getLastRunTimestamp().map(Object::toString).orElse("never"));
        sender.sendMessage("Conflicts: " + api.getConflicts().size()
                + " | Security risks: " + api.getSecurityRisks().size()
                + " | Recommendations: " + api.getRecommendations().size());

        var recs = api.getRecommendations();
        int shown = Math.min(3, recs.size());
        for (int i = 0; i < shown; i++) {
            Recommendation r = recs.get(i);
            sender.sendMessage(" - [" + r.severity() + "] " + r.title());
        }
        // Force a fresh run on demand:
        if (args.length > 0 && args[0].equalsIgnoreCase("scan")) {
            sender.sendMessage("Running a fresh analysis...");
            sender.sendMessage("New overall severity: " + api.runDiagnostics().overallSeverity());
        }
        return true;
    }
}
