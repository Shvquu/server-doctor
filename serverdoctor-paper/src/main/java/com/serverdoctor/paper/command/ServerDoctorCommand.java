package com.serverdoctor.paper.command;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import com.serverdoctor.core.messages.MessageStore;
import com.serverdoctor.paper.gui.MenuType;
import com.serverdoctor.paper.gui.ServerDoctorGui;
import com.serverdoctor.storage.StorageProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** /serverdoctor - read-only Diagnose-Ausgabe; alle Texte aus messages.yml. */
public final class ServerDoctorCommand implements CommandExecutor, TabCompleter {

    private final ServerDoctorApi api;
    private final StorageProvider storage;
    private final MessageStore msg;
    private final Runnable reloadHandler;
    private final ServerDoctorGui gui;

    public ServerDoctorCommand(ServerDoctorApi api, StorageProvider storage, MessageStore msg, Runnable reloadHandler, ServerDoctorGui gui) {
        this.api = api;
        this.storage = storage;
        this.msg = msg;
        this.reloadHandler = reloadHandler;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender s, @NonNull Command cmd, @NonNull String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "scan"            -> scan(s);
            case "report"          -> report(s);
            case "tps"             -> tps(s);
            case "conflicts"       -> conflicts(s);
            case "security"        -> security(s);
            case "recommendations",
                 "recs"            -> recommendations(s);
            case "history",
                 "hist"            -> history(s);
            case "reload"          -> reload(s);
            case "gui",
                 "menu"            -> openGui(s);
            default                -> help(s);
        }
        return true;
    }

    private void scan(CommandSender s) {
        sendKey(s, true, "command.scan.running");
        DiagnosticReport r = api.runDiagnostics();
        send(s, true, msg.get("command.scan.summary",
                "status", status(r.overallSeverity()),
                "conflicts", r.conflicts().size(),
                "risks", r.securityRisks().size(),
                "recommendations", r.recommendations().size()));
        sendKey(s, true, "command.scan.details");
    }

    private void report(CommandSender s) {
        var opt = api.getLatestReport();
        if (opt.isEmpty()) { sendKey(s, true, "command.report.none"); return; }
        DiagnosticReport r = opt.get();
        sendKey(s, false, "command.report.header");
        send(s, false, msg.get("command.report.status", "status", status(r.overallSeverity())));
        for (var result : r.results()) {
            for (Finding f : result.findings()) {
                send(s, false, "&8» " + sev(f.severity()) + " &7" + f.message());
            }
        }
        send(s, false, msg.get("command.report.recommendations", "count", r.recommendations().size()));
    }

    private void tps(CommandSender s) { printPerformance(s, api.getPerformanceSnapshot()); }

    private void printPerformance(CommandSender s, PerformanceSnapshot p) {
        send(s, false, msg.get("command.performance.line",
                "tps", num(p.tps1m()), "mspt", num(p.mspt()),
                "ram_used", p.memory().usedMb(), "ram_max", p.memory().maxMb(),
                "players", p.onlinePlayers()));
    }

    private void conflicts(CommandSender s) {
        List<ConflictReport> c = api.getConflicts();
        if (c.isEmpty()) { sendKey(s, true, "command.conflicts.none"); return; }
        c.forEach(x -> send(s, false, "&8» " + sev(x.severity()) + " "
                + msg.get("command.conflicts.line", "a", x.pluginA(), "b", x.pluginB(), "description", x.description())));
    }

    private void security(CommandSender s) {
        List<SecurityRisk> risks = api.getSecurityRisks();
        if (risks.isEmpty()) { sendKey(s, true, "command.security.none"); return; }
        risks.forEach(x -> send(s, false, "&8» " + sev(x.severity()) + " "
                + msg.get("command.security.line", "plugin", x.pluginName(), "description", x.description())));
    }

    private void recommendations(CommandSender s) {
        List<Recommendation> recs = api.getRecommendations();
        if (recs.isEmpty()) { sendKey(s, true, "command.recommendations.none"); return; }
        recs.forEach(x -> send(s, false, "&8» " + sev(x.severity()) + " "
                + msg.get("command.recommendations.line", "title", x.title(), "description", x.description())));
    }

    private void history(CommandSender s) {
        var snapshots = storage.performance().recent(10);
        if (snapshots.isEmpty()) { sendKey(s, true, "command.history.none"); return; }
        sendKey(s, false, "command.history.header");
        for (PerformanceSnapshot p : snapshots) {
            send(s, false, msg.get("command.history.line",
                    "time", p.capturedAt(), "tps", num(p.tps1m()),
                    "ram", p.memory().usedMb(), "players", p.onlinePlayers()));
        }
    }

    private void reload(CommandSender s) {
        reloadHandler.run();
        sendKey(s, true, "command.reload.success");
    }

    private void help(CommandSender s) {
        sendKey(s, false, "command.help.header");
        for (String k : List.of("scan", "report", "tps", "conflicts", "security", "recs", "history")) {
            sendKey(s, false, "command.help." + k);
        }
    }

    private void openGui(CommandSender s) {
        if (gui == null) { send(s, true, "&cThe GUI is disabled."); return; }
        if (!(s instanceof Player p)) { send(s, true, "&cThis command is only for players."); return; }
        gui.open(p, MenuType.MAIN);
    }

    // -- Helfer --

    private String status(Severity sev) { return sev(sev) + " &7" + sev.name(); }

    private String sev(Severity sev) {
        return switch (sev) {
            case OK, INFO -> "&a●";
            case LOW      -> "&e●";
            case MEDIUM   -> "&6●";
            case HIGH     -> "&c●";
            case CRITICAL -> "&4●";
        };
    }

    private static String num(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.1f", v);
    }

    private void sendKey(CommandSender s, boolean withPrefix, String key) { send(s, withPrefix, msg.get(key)); }

    private void send(CommandSender s, boolean withPrefix, String line) {
        String full = (withPrefix ? msg.raw("prefix") : "") + line;
        s.sendMessage(ChatColor.translateAlternateColorCodes('&', full));
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender s, @NonNull Command cmd, @NonNull String label, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            Stream.of("scan", "report", "tps", "conflicts", "security", "recs", "history", "reload", "gui", "menu")
                    .filter(o -> o.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .forEach(opts::add);
            return opts;
        }
        return List.of();
    }
}
