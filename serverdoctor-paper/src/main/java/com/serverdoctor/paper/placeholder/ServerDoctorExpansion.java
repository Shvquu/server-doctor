package com.serverdoctor.paper.placeholder;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.common.model.PerformanceSnapshot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class ServerDoctorExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final ServerDoctorApi api;

    public ServerDoctorExpansion(Plugin plugin, ServerDoctorApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "serverdoctor";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "ServerDoctor" : String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        PerformanceSnapshot p = api.getPerformanceSnapshot();
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "tps"             -> fmt(p.tps1m());
            case "mspt"            -> fmt(p.mspt());
            case "memory"          -> p.memory().usedMb() + "/" + p.memory().maxMb() + "MB";
            case "memory_used"     -> String.valueOf(p.memory().usedMb());
            case "memory_max"      -> String.valueOf(p.memory().maxMb());
            case "players"         -> String.valueOf(p.onlinePlayers());
            case "conflicts"       -> String.valueOf(api.getConflicts().size());
            case "security_risks"  -> String.valueOf(api.getSecurityRisks().size());
            case "recommendations" -> String.valueOf(api.getRecommendations().size());
            case "status"          -> api.getLatestReport()
                    .map(r -> r.overallSeverity().name()).orElse("UNKNOWN");
            default -> null;
        };
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.2f", v);
    }
}
