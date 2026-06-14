package com.serverdoctor.velocity;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.core.messages.MessageStore;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.util.Locale;

/** /serverdoctor auf dem Proxy - Texte aus messages.yml (Farbcodes werden entfernt). */
public final class ServerDoctorVelocityCommand implements SimpleCommand {

    private final ServerDoctorApi api;
    private final MessageStore msg;

    public ServerDoctorVelocityCommand(ServerDoctorApi api, MessageStore msg) {
        this.api = api;
        this.msg = msg;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "scan" -> {
                DiagnosticReport r = api.runDiagnostics();
                send(src, msg.get("command.scan.summary",
                        "status", r.overallSeverity(),
                        "conflicts", r.conflicts().size(),
                        "risks", r.securityRisks().size(),
                        "recommendations", r.recommendations().size()));
            }
            case "report" -> {
                var opt = api.getLatestReport();
                if (opt.isEmpty()) { send(src, msg.raw("command.report.none")); return; }
                DiagnosticReport r = opt.get();
                send(src, msg.get("command.report.status", "status", r.overallSeverity()));
                r.results().forEach(res -> res.findings().forEach(f ->
                        send(src, "- [" + f.severity() + "] " + f.message())));
            }
            case "tps" -> {
                PerformanceSnapshot p = api.getPerformanceSnapshot();
                send(src, msg.get("command.performance.line",
                        "tps", num(p.tps1m()), "mspt", num(p.mspt()),
                        "ram_used", p.memory().usedMb(), "ram_max", p.memory().maxMb(),
                        "players", p.onlinePlayers()));
            }
            case "conflicts" -> {
                var c = api.getConflicts();
                if (c.isEmpty()) { send(src, msg.raw("command.conflicts.none")); return; }
                c.forEach(x -> send(src, msg.get("command.conflicts.line",
                        "a", x.pluginA(), "b", x.pluginB(), "description", x.description())));
            }
            case "security" -> {
                var risks = api.getSecurityRisks();
                if (risks.isEmpty()) { send(src, msg.raw("command.security.none")); return; }
                risks.forEach(x -> send(src, msg.get("command.security.line",
                        "plugin", x.pluginName(), "description", x.description())));
            }
            case "recs" -> {
                var recs = api.getRecommendations();
                if (recs.isEmpty()) { send(src, msg.raw("command.recommendations.none")); return; }
                recs.forEach(x -> send(src, msg.get("command.recommendations.line",
                        "title", x.title(), "description", x.description())));
            }
            default -> {
                send(src, msg.raw("command.help.header"));
                for (String k : new String[]{"scan", "report", "tps", "conflicts", "security", "recs"}) {
                    send(src, msg.raw("command.help." + k));
                }
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("serverdoctor.admin");
    }

    private void send(CommandSource src, String line) {
        src.sendMessage(Component.text(strip(line)));
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)&[0-9a-fk-or]", "");
    }

    private static String num(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.1f", v);
    }
}
