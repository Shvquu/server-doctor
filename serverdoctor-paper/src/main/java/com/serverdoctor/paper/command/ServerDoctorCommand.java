package com.serverdoctor.paper.command;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.serverdoctor.common.model.Severity;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** /serverdoctor - read-only Diagnose-Ausgabe im Chat/Konsole. */
public final class ServerDoctorCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§bServerDoctor§8] §r";
    private final ServerDoctorApi api;

    public ServerDoctorCommand(ServerDoctorApi api) { this.api = api; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "scan"            -> scan(s);
            case "report"          -> report(s);
            case "tps"             -> tps(s);
            case "conflicts"       -> conflicts(s);
            case "security"        -> security(s);
            case "recommendations",
                 "recs"            -> recommendations(s);
            default                -> help(s);
        }
        return true;
    }

    private void scan(CommandSender s) {
        s.sendMessage(PREFIX + "§7Analyse läuft …");
        DiagnosticReport report = api.runDiagnostics();
        s.sendMessage(PREFIX + "Fertig. Gesamtstatus: " + color(report.overallSeverity()));
        s.sendMessage(PREFIX + "§7Konflikte: §f" + report.conflicts().size()
                + " §7· Risiken: §f" + report.securityRisks().size()
                + " §7· Empfehlungen: §f" + report.recommendations().size());
        s.sendMessage(PREFIX + "§7Details: §f/serverdoctor report");
    }

    private void report(CommandSender s) {
        var opt = api.getLatestReport();
        if (opt.isEmpty()) { s.sendMessage(PREFIX + "§7Noch kein Report. Erst §f/serverdoctor scan§7."); return; }
        DiagnosticReport r = opt.get();
        s.sendMessage(PREFIX + "§lDiagnose-Report");
        s.sendMessage("§8» §7Status: " + color(r.overallSeverity()));
        printPerformance(s, r.performance());
        for (var result : r.results()) {
            for (Finding f : result.findings()) {
                s.sendMessage("§8» " + sev(f.severity()) + " §7" + f.message());
            }
        }
        s.sendMessage("§8» §7Empfehlungen: §f" + r.recommendations().size()
                + " §7(§f/serverdoctor recs§7)");
    }

    private void tps(CommandSender s) {
        printPerformance(s, api.getPerformanceSnapshot());
    }

    private void printPerformance(CommandSender s, PerformanceSnapshot p) {
        String tps = Double.isNaN(p.tps1m()) ? "n/a" : String.format(Locale.ROOT, "%.1f", p.tps1m());
        String mspt = Double.isNaN(p.mspt()) ? "n/a" : String.format(Locale.ROOT, "%.1fms", p.mspt());
        s.sendMessage("§8» §7TPS: §f" + tps + " §7· MSPT: §f" + mspt
                + " §7· RAM: §f" + p.memory().usedMb() + "/" + p.memory().maxMb() + "MB"
                + " §7· Spieler: §f" + p.onlinePlayers());
    }

    private void conflicts(CommandSender s) {
        List<ConflictReport> c = api.getConflicts();
        if (c.isEmpty()) { s.sendMessage(PREFIX + "§aKeine bekannten Konflikte."); return; }
        c.forEach(x -> s.sendMessage("§8» " + sev(x.severity()) + " §f" + x.pluginA()
                + " §7+ §f" + x.pluginB() + " §8- §7" + x.description()));
    }

    private void security(CommandSender s) {
        List<SecurityRisk> risks = api.getSecurityRisks();
        if (risks.isEmpty()) { s.sendMessage(PREFIX + "§aKeine Risiken gemeldet."); return; }
        risks.forEach(x -> s.sendMessage("§8» " + sev(x.severity()) + " §f" + x.pluginName()
                + " §8- §7" + x.description()));
    }

    private void recommendations(CommandSender s) {
        List<Recommendation> recs = api.getRecommendations();
        if (recs.isEmpty()) { s.sendMessage(PREFIX + "§aKeine Empfehlungen."); return; }
        recs.forEach(x -> s.sendMessage("§8» " + sev(x.severity()) + " §f" + x.title()
                + " §8- §7" + x.description()));
    }

    private void help(CommandSender s) {
        s.sendMessage(PREFIX + "§lBefehle");
        s.sendMessage("§8» §f/serverdoctor scan §7- vollständige Analyse");
        s.sendMessage("§8» §f/serverdoctor report §7- letzten Report anzeigen");
        s.sendMessage("§8» §f/serverdoctor tps §7- Live-Performance");
        s.sendMessage("§8» §f/serverdoctor conflicts §7- Konflikte");
        s.sendMessage("§8» §f/serverdoctor security §7- Sicherheits-/Wartungsrisiken");
        s.sendMessage("§8» §f/serverdoctor recs §7- Empfehlungen");
    }

    private String color(Severity sev) { return sev(sev) + " §7" + sev.name(); }

    private String sev(Severity sev) {
        return switch (sev) {
            case OK, INFO -> "§a●";
            case LOW      -> "§e●";
            case MEDIUM   -> "§6●";
            case HIGH     -> "§c●";
            case CRITICAL -> "§4●";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            Stream.of("scan", "report", "tps", "conflicts", "security", "recs")
                    .filter(o -> o.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .forEach(opts::add);
            return opts;
        }
        return List.of();
    }
}
