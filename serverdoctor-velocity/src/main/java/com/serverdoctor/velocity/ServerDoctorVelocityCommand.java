package com.serverdoctor.velocity;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;

/** /serverdoctor auf dem Proxy - read-only Ausgabe. */
public final class ServerDoctorVelocityCommand implements SimpleCommand {

    private final ServerDoctorApi api;

    public ServerDoctorVelocityCommand(ServerDoctorApi api) { this.api = api; }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "scan" -> {
                DiagnosticReport r = api.runDiagnostics();
                send(src, "Analyse fertig. Status: " + r.overallSeverity()
                        + " | Konflikte: " + r.conflicts().size()
                        + " | Risiken: " + r.securityRisks().size()
                        + " | Empfehlungen: " + r.recommendations().size());
            }
            case "report" -> {
                var opt = api.getLatestReport();
                if (opt.isEmpty()) { send(src, "Noch kein Report - erst /serverdoctor scan."); return; }
                DiagnosticReport r = opt.get();
                send(src, "Status: " + r.overallSeverity());
                r.results().forEach(res -> res.findings().forEach(f ->
                        send(src, "- [" + f.severity() + "] " + f.message())));
            }
            case "tps" -> {
                PerformanceSnapshot p = api.getPerformanceSnapshot();
                send(src, "Proxy-RAM: " + p.memory().usedMb() + "/" + p.memory().maxMb()
                        + "MB | Threads: " + p.threadCount() + " | Spieler: " + p.onlinePlayers());
            }
            case "conflicts" -> {
                var c = api.getConflicts();
                if (c.isEmpty()) { send(src, "Keine bekannten Konflikte."); return; }
                c.forEach(x -> send(src, "- " + x.pluginA() + " + " + x.pluginB() + ": " + x.description()));
            }
            case "security" -> {
                var risks = api.getSecurityRisks();
                if (risks.isEmpty()) { send(src, "Keine Risiken gemeldet."); return; }
                risks.forEach(x -> send(src, "- " + x.pluginName() + ": " + x.description()));
            }
            case "recs" -> {
                var recs = api.getRecommendations();
                if (recs.isEmpty()) { send(src, "Keine Empfehlungen."); return; }
                recs.forEach(x -> send(src, "- " + x.title()));
            }
            default -> {
                send(src, "ServerDoctor: /serverdoctor <scan|report|tps|conflicts|security|recs>");
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("serverdoctor.admin");
    }

    private void send(CommandSource src, String line) {
        src.sendMessage(Component.text(line));
    }
}
