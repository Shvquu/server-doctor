package com.serverdoctor.rest;

import com.serverdoctor.api.ServerDoctorApi;
import com.serverdoctor.api.module.AnalysisResult;
import com.serverdoctor.api.module.DiagnosticReport;
import com.serverdoctor.common.model.ConflictReport;
import com.serverdoctor.common.model.Finding;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.Recommendation;
import com.serverdoctor.common.model.SecurityRisk;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Lightweight, read-only HTTP/JSON API built on the JDK's {@code com.sun.net.httpserver}.
 * No external dependencies. Exposes the latest analysis data; it never triggers actions
 * or mutates anything.
 *
 * <p>Endpoints (all GET): {@code /health} (no auth), {@code /performance}, {@code /conflicts},
 * {@code /security}, {@code /recommendations}, {@code /report}, {@code /metrics} (Prometheus). If a token is configured,
 * every endpoint except {@code /health} requires {@code Authorization: Bearer <token>}.
 */
public final class RestApiServer {

    private final ServerDoctorApi api;
    private final RestApiConfig config;
    private final String version;
    private final Consumer<String> log;

    private HttpServer server;

    public RestApiServer(ServerDoctorApi api, RestApiConfig config, String version, Consumer<String> log) {
        this.api = api;
        this.config = config;
        this.version = version == null ? "unknown" : version;
        this.log = log == null ? m -> {} : log;
    }

    public void start() throws IOException {
        if (!config.enabled()) return;

        server = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 0);
        AtomicInteger n = new AtomicInteger();
        server.setExecutor(Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "serverdoctor-rest-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        }));

        server.createContext("/health", this::health);
        server.createContext("/performance", auth(this::performance));
        server.createContext("/conflicts", auth(this::conflicts));
        server.createContext("/security", auth(this::security));
        server.createContext("/recommendations", auth(this::recommendations));
        server.createContext("/report", auth(this::report));
        server.createContext("/metrics", auth(this::metrics));

        server.start();
        log.accept("REST API listening on http://" + config.host() + ":" + config.port()
                + (config.requiresAuth() ? " (token required)" : ""));
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    // ---- handlers -----------------------------------------------------------

    private void health(HttpExchange ex) throws IOException {
        respond(ex, 200, J.obj("status", J.s("ok"), "name", J.s("ServerDoctor"), "version", J.s(version)));
    }

    private void performance(HttpExchange ex) throws IOException {
        if (notGet(ex)) return;
        respond(ex, 200, performanceJson(api.getPerformanceSnapshot()));
    }

    private void conflicts(HttpExchange ex) throws IOException {
        if (notGet(ex)) return;
        respond(ex, 200, J.arr(api.getConflicts().stream().map(RestApiServer::conflictJson).toList()));
    }

    private void security(HttpExchange ex) throws IOException {
        if (notGet(ex)) return;
        respond(ex, 200, J.arr(api.getSecurityRisks().stream().map(RestApiServer::riskJson).toList()));
    }

    private void recommendations(HttpExchange ex) throws IOException {
        if (notGet(ex)) return;
        respond(ex, 200, J.arr(api.getRecommendations().stream().map(RestApiServer::recommendationJson).toList()));
    }

    private void report(HttpExchange ex) throws IOException {
        if (notGet(ex)) return;
        Optional<DiagnosticReport> latest = api.getLatestReport();
        if (latest.isEmpty()) {
            respond(ex, 404, J.obj("error", J.s("no report yet")));
            return;
        }
        respond(ex, 200, reportJson(latest.get()));
    }

    private void metrics(HttpExchange ex) throws IOException {
        if (notGet(ex)) return;
        String body = PrometheusFormatter.render(api.getPerformanceSnapshot(), api.getLatestReport());
        respondText(ex, 200, body);
    }

    // ---- serialization ------------------------------------------------------

    private static double tpsAt(PerformanceSnapshot p, int i) {
        double[] t = p.tps();
        return t != null && t.length > i ? t[i] : Double.NaN;
    }

    private static String performanceJson(PerformanceSnapshot p) {
        return J.obj(
                "tps1m", J.n(p.tps1m()),
                "tps5m", J.n(tpsAt(p, 1)),
                "tps15m", J.n(tpsAt(p, 2)),
                "mspt", J.n(p.mspt()),
                "memory", J.obj(
                        "usedBytes", J.n(p.memory().usedBytes()),
                        "committedBytes", J.n(p.memory().committedBytes()),
                        "maxBytes", J.n(p.memory().maxBytes()),
                        "usedMb", J.n(p.memory().usedMb()),
                        "maxMb", J.n(p.memory().maxMb()),
                        "usedRatio", J.n(p.memory().usedRatio()),
                        "gcCount", J.n(p.memory().gcCount()),
                        "gcTimeMs", J.n(p.memory().gcTimeMs())),
                "threads", J.n(p.threadCount()),
                "players", J.n(p.onlinePlayers()),
                "capturedAt", J.s(p.capturedAt().toString()));
    }

    private static String conflictJson(ConflictReport c) {
        return J.obj(
                "id", J.s(c.id()),
                "pluginA", J.s(c.pluginA()),
                "pluginB", J.s(c.pluginB()),
                "severity", J.s(c.severity().name()),
                "description", J.s(c.description()));
    }

    private static String riskJson(SecurityRisk r) {
        return J.obj(
                "plugin", J.s(r.pluginName()),
                "type", J.s(r.type().name()),
                "severity", J.s(r.severity().name()),
                "description", J.s(r.description()));
    }

    private static String recommendationJson(Recommendation r) {
        return J.obj(
                "id", J.s(r.id()),
                "category", J.s(r.category().name()),
                "severity", J.s(r.severity().name()),
                "title", J.s(r.title()),
                "description", J.s(r.description()));
    }

    private static String findingJson(Finding f) {
        return J.obj(
                "scanner", J.s(f.scannerId()),
                "severity", J.s(f.severity().name()),
                "message", J.s(f.message()));
    }

    private static String resultJson(AnalysisResult r) {
        return J.obj(
                "module", J.s(r.moduleId()),
                "severity", J.s(r.severity().name()),
                "findings", J.arr(r.findings().stream().map(RestApiServer::findingJson).toList()));
    }

    private static String reportJson(DiagnosticReport report) {
        return J.obj(
                "timestamp", J.s(report.timestamp().toString()),
                "overallSeverity", J.s(report.overallSeverity().name()),
                "performance", performanceJson(report.performance()),
                "results", J.arr(report.results().stream().map(RestApiServer::resultJson).toList()),
                "conflicts", J.arr(report.conflicts().stream().map(RestApiServer::conflictJson).toList()),
                "securityRisks", J.arr(report.securityRisks().stream().map(RestApiServer::riskJson).toList()),
                "recommendations", J.arr(report.recommendations().stream().map(RestApiServer::recommendationJson).toList()));
    }

    // ---- plumbing -----------------------------------------------------------

    private HttpHandler auth(HttpHandler inner) {
        return exchange -> {
            if (config.requiresAuth()) {
                String header = exchange.getRequestHeaders().getFirst("Authorization");
                if (header == null || !header.equals("Bearer " + config.token())) {
                    respond(exchange, 401, J.obj("error", J.s("unauthorized")));
                    return;
                }
            }
            inner.handle(exchange);
        };
    }

    private boolean notGet(HttpExchange ex) throws IOException {
        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) return false;
        respond(ex, 405, J.obj("error", J.s("method not allowed")));
        return true;
    }

    private void respondText(HttpExchange ex, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
