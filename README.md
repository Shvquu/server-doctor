# ServerDoctor

[![Build](https://github.com/Shvquu/server-doctor/actions/workflows/workflow.yml/badge.svg)](https://github.com/Shvquu/server-doctor/actions/workflows/workflow.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.6.0-informational.svg)](https://github.com/Shvquu/server-doctor/releases)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper/Folia](https://img.shields.io/badge/Paper%2FFolia-1.21.x-brightgreen.svg)](https://papermc.io/)
[![Velocity](https://img.shields.io/badge/Velocity-3.5-brightgreen.svg)](https://papermc.io/software/velocity)

Read-only analysis, diagnostics and monitoring platform for Minecraft servers and proxies.
**Analyzes, evaluates, recommends, warns — never changes anything on the server.**

ServerDoctor runs the same engine on **Paper/Folia** and **Velocity**, shipped as a single
jar. The core is platform-neutral; thin adapters wire it into each platform. A strict
architecture boundary (enforced as a build breaker) guarantees the analysis layer can never
reach into a platform SDK and the platform adapters stay read-only.

## Modules

| Module | Contents | Status |
|---|---|---|
| `serverdoctor-common` | Domain models, utilities | ✅ verified |
| `serverdoctor-api` | Public contract, events, module SPI | ✅ verified |
| `serverdoctor-core` | Engine, 5 scanners, recommendations, conflict DB, update checker | ✅ verified |
| `serverdoctor-storage` | StorageProvider, 5 repositories — In-Memory, SQLite, PostgreSQL, MariaDB, MongoDB | ✅ verified |
| `serverdoctor-testing` | Fake-platform fixtures, JUnit 5 suite, ArchUnit rules | ✅ verified |
| `serverdoctor-paper` | Bukkit/Paper/Folia adapter, command, PlaceholderAPI bridge, storage wiring | ⚙️ buildable |
| `serverdoctor-velocity` | Velocity adapter, command, storage wiring | ⚙️ buildable |
| `serverdoctor-universal` | Bundles Paper + Velocity into one shaded jar | ⚙️ buildable |

Included scanners: **Plugin**, **Dependency**, **Conflict**, **Performance**, **Security**.

## Building

Requirements: JDK 21, Gradle 8.8+ (or run `./gradlew` — the wrapper is included).

```bash
gradle :serverdoctor-universal:shadowJar
```

Output: `serverdoctor-universal/build/libs/serverdoctor-0.6.0.jar`

That one jar carries both a `plugin.yml` (Paper/Folia) and a `velocity-plugin.json`
(Velocity), so the same file drops into the `plugins/` folder of either a Paper 1.21.x
server or a Velocity proxy.

> The jar bundles the database drivers (SQLite, PostgreSQL, MariaDB, MongoDB). The MongoDB
> driver is the largest of these; if you only use SQL backends you can drop the
> `mongodb-driver-sync` dependency to keep the jar smaller.

## Configuration

On first start a `config.yml` is created in the plugin's data folder (written automatically
on Paper, copied from the jar on Velocity). It selects the storage backend and holds the
credentials for the server-based backends:

```yaml
storage:
  # memory | sqlite | postgresql | mariadb | mongodb
  type: sqlite

  sqlite:
    file: "serverdoctor.db"

  postgresql:
    host: "localhost"
    port: 5432
    database: "serverdoctor"
    username: "serverdoctor"
    password: "changeme"

  mariadb:
    host: "localhost"
    port: 3306
    database: "serverdoctor"
    username: "serverdoctor"
    password: "changeme"

  mongodb:
    # If set, takes precedence over the discrete fields below.
    connection-string: ""
    host: "localhost"
    port: 27017
    database: "serverdoctor"
    username: "serverdoctor"
    password: "changeme"
    auth-database: "admin"
```

If the configured backend cannot be reached at startup, ServerDoctor falls back to SQLite
and then to In-Memory rather than blocking the server from starting.

On a network, a shared PostgreSQL/MariaDB/MongoDB is the point: multiple ServerDoctor
instances (proxy plus backend servers) write into the same database. SQLite and In-Memory
are best for a single node.

## Commands

```
/serverdoctor scan            # run a full analysis
/serverdoctor report          # show the latest report
/serverdoctor tps             # live performance (TPS/MSPT/RAM)
/serverdoctor conflicts       # detected plugin conflicts
/serverdoctor security        # security and maintenance risks
/serverdoctor recs            # generated recommendations
/serverdoctor history         # stored performance history (Paper/Folia)
/serverdoctor reload          # reload messages.yml
```

Aliases: `/sd`, `/doctor` · Permission: `serverdoctor.admin` (default: op).
An asynchronous background scan also runs every 5 minutes.

## Tests

Unit tests (JUnit 5) live in `src/test` of the respective modules; fixtures and the
ArchUnit architecture tests are in `serverdoctor-testing`.

```bash
gradle test
```

Coverage includes version/severity logic, the AnalysisResult builder, ScannerRegistry
capability gating, the EventBus (including error isolation), all scanner thresholds, the
RecommendationEngine, the analysis engine end-to-end, and storage round-trips. The ArchUnit
rules enforce, as build breakers: no platform SDK in Core/Common/API/Storage, the Clean
Architecture dependency rule, and the read-only invariant of the platform adapters.

## Integration for third-party plugins (≤ 5 lines)

```java
import com.serverdoctor.api.ServerDoctorProvider;
import com.serverdoctor.api.event.PluginConflictDetectedEvent;

var api = ServerDoctorProvider.get();
double tps = api.getPerformanceSnapshot().tps1m();
api.events().subscribe(PluginConflictDetectedEvent.class,
        e -> getLogger().warning("Conflict: " + e.conflict().description()));
```

Register your own scanner:

```java
api.registerModule(new AnalysisModule() {
    public String id() { return "my-scanner"; }
    public AnalysisResult analyze(ServerContext ctx) {
        return AnalysisResult.builder(id())
                .finding(new Finding(id(), Severity.INFO,
                        ctx.plugins().size() + " plugins seen"))
                .build();
    }
});
```

## Not included yet (upcoming iterations)

- `bungeecord` platform adapter
- `serverdoctor-rest-api` (HTTP/JSON) and `serverdoctor-webhook` (Discord/Slack/Teams)
- `serverdoctor-example-plugin` (reference integration)
- A real security advisory source (currently a metadata heuristic, no invented CVE database)
