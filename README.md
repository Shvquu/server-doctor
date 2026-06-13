# ServerDoctor

Read-only Analyse-, Diagnose- und Monitoring-Plattform für Minecraft-Server.
**Analysiert, bewertet, empfiehlt, warnt — verändert niemals etwas am Server.**

Dies ist das **lauffähige Fundament** (Phase „Plugin v0.1") aus dem Architekturdokument:
vier Module, ein baubares Paper-Plugin mit echten Scannern, Public-API, Event-System
und In-Game-Command.

## Module in diesem Build

| Modul | Inhalt | Status |
|---|---|---|
| `serverdoctor-common` | Domain-Modelle, Util | ✅ kompiliert (verifiziert) |
| `serverdoctor-api` | Public Contract, Events, Module-SPI | ✅ kompiliert (verifiziert) |
| `serverdoctor-core` | Engine, 5 Scanner, Recommendation, ConflictDB | ✅ kompiliert (verifiziert) |
| `serverdoctor-storage` | StorageProvider, 5 Repositories, SQLite + In-Memory | ✅ verifiziert (Memory live, SQLite-Schema gegen echte Engine) |
| `serverdoctor-testing` | Fake-Plattform-Fixtures, JUnit-5-Suite, ArchUnit-Regeln | ✅ verifiziert (40 Assertions framework-frei nachgewiesen) |
| `serverdoctor-paper` | Bukkit/Paper-Adapter, Plugin-Main, Command, Storage-Wiring | ⚙️ baubar (Paper-API nötig) |

Enthaltene Scanner: **Plugin**, **Dependency**, **Conflict**, **Performance**, **Security**.

## Bauen

Voraussetzungen: JDK 21, Gradle 8.8+ (oder `gradle wrapper` einmalig ausführen).

```bash
gradle :serverdoctor-paper:shadowJar
```

Ergebnis: `serverdoctor-paper/build/libs/ServerDoctor-0.1.0-SNAPSHOT.jar`
(enthält common/api/core gebündelt). In den `plugins/`-Ordner eines Paper-1.21.x-Servers legen.

## Befehle

```
/serverdoctor scan            # vollständige Analyse ausführen
/serverdoctor report          # letzten Report anzeigen
/serverdoctor tps             # Live-Performance (TPS/MSPT/RAM)
/serverdoctor conflicts       # erkannte Plugin-Konflikte
/serverdoctor security        # Sicherheits-/Wartungsrisiken
/serverdoctor recs            # generierte Empfehlungen
/serverdoctor history         # gespeicherte Performance-Historie
```
Alias: `/sd`, `/doctor` · Permission: `serverdoctor.admin` (default: op).
Zusätzlich läuft alle 5 Minuten ein asynchroner Hintergrund-Scan.

## Tests

Unit-Tests (JUnit 5) liegen in `src/test` der jeweiligen Module; Fixtures und die
ArchUnit-Architekturtests im Modul `serverdoctor-testing`.

```bash
gradle test
```

Abgedeckt: Versions/Severity-Logik, AnalysisResult-Builder, ScannerRegistry-Capability-Gating,
EventBus (inkl. Fehler-Isolation), alle Scanner-Schwellen, RecommendationEngine, Analysis-Engine
end-to-end, Storage (Memory + SQLite-Roundtrip via `jdbc:sqlite::memory:`). Die ArchUnit-Regeln
erzwingen als Build-Brecher: kein Plattform-SDK in Core/Common/API/Storage, die Clean-Architecture-
Dependency-Rule und die Read-only-Invariante der Plattform-Adapter.

## Integration für Fremd-Plugins (≤ 5 Zeilen)

```java
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.PluginConflictDetectedEvent;

var api = ServerDoctorProvider.get();
double tps = api.getPerformanceSnapshot().tps1m();
api.events().subscribe(PluginConflictDetectedEvent.class,
        e -> getLogger().warning("Konflikt: " + e.conflict().description()));
```

Eigenen Scanner registrieren:

```java
api.registerModule(new AnalysisModule() {
    public String id() { return "mein-scanner"; }
    public AnalysisResult analyze(ServerContext ctx) {
        return AnalysisResult.builder(id())
                .finding(new Finding(id(), Severity.INFO,
                        ctx.plugins().size() + " Plugins gesehen"))
                .build();
    }
});
```

## Bewusst noch NICHT enthalten (nächste Iterationen)

- Module `folia`, `velocity`, `bungeecord` (Plattform-Adapter)
- `serverdoctor-rest-api` (HTTP/JSON) und `serverdoctor-webhook` (Discord/Slack/Teams)
- Update-Checker (Modrinth/Hangar/SpigotMC/GitHub), PlaceholderAPI-Bridge
- `serverdoctor-example-plugin` (Referenz-Integration)
- Echte Security-AdvisorySource (aktuell nur Metadaten-Heuristik, keine erfundene CVE-DB)
