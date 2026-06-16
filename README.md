# ServerDoctor

[![Build](https://github.com/Shvquu/server-doctor/actions/workflows/workflow.yml/badge.svg)](https://github.com/Shvquu/server-doctor/actions/workflows/workflow.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.8.0-informational.svg)](https://github.com/Shvquu/server-doctor/releases)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper/Folia](https://img.shields.io/badge/Paper%2FFolia-1.21.x-brightgreen.svg)](https://papermc.io/)
[![Velocity](https://img.shields.io/badge/Velocity-3.4-brightgreen.svg)](https://papermc.io/software/velocity)

Read-only analysis, diagnostics and monitoring platform for Minecraft servers and proxies.
**Analyzes, evaluates, recommends, warns — never changes anything on the server.**

ServerDoctor runs the same engine on **Paper/Folia** and **Velocity**, shipped as a single
jar. The core is platform-neutral; thin adapters wire it into each platform. A strict
architecture boundary (enforced as a build breaker) guarantees the analysis layer can never
reach into a platform SDK and the platform adapters stay read-only.

## What it does

- **Five scanners** — Plugin, Dependency, Conflict, Performance, Security.
- **Recommendations** derived from findings (data only — never executed automatically).
- **Performance history** — TPS/MSPT/RAM snapshots persisted over time.
- **Pluggable storage** — In-Memory, SQLite, PostgreSQL, MariaDB, MongoDB.
- **REST API** — read-only HTTP/JSON access to the latest data.
- **Webhooks** — Discord, Slack and Microsoft Teams notifications on status changes.
- **Public API + event bus**, **PlaceholderAPI** support, and a GitHub **update checker**.

## Modules

| Module | Contents | Status |
|---|---|---|
| `serverdoctor-common` | Domain models, utilities | ✅ verified |
| `serverdoctor-api` | Public contract, events, module SPI | ✅ verified |
| `serverdoctor-core` | Engine, 5 scanners, recommendations, conflict DB, update checker | ✅ verified |
| `serverdoctor-storage` | StorageProvider, 5 repositories — In-Memory, SQLite, PostgreSQL, MariaDB, MongoDB | ✅ verified |
| `serverdoctor-rest-api` | Read-only HTTP/JSON API (JDK HttpServer, no extra deps) | ✅ verified |
| `serverdoctor-webhook` | Discord/Slack/Teams notifications (JDK HttpClient, no extra deps) | ✅ verified |
| `serverdoctor-testing` | Fake-platform fixtures, JUnit 5 suite, ArchUnit rules | ✅ verified |
| `serverdoctor-paper` | Bukkit/Paper/Folia adapter, command, PlaceholderAPI bridge, service wiring | ⚙️ buildable |
| `serverdoctor-velocity` | Velocity adapter, command, service wiring | ⚙️ buildable |
| `serverdoctor-universal` | Bundles Paper + Velocity into one shaded jar | ⚙️ buildable |

Included scanners: **Plugin**, **Dependency**, **Conflict**, **Performance**, **Security**.

## Building

Requirements: JDK 21, Gradle 8.8+ (or run `./gradlew` — the wrapper is included).

```bash
gradle :serverdoctor-universal:shadowJar
```

Output: `serverdoctor-universal/build/libs/serverdoctor-0.8.0.jar`

That one jar carries both a `plugin.yml` (Paper/Folia) and a `velocity-plugin.json`
(Velocity), so the same file drops into the `plugins/` folder of either a Paper 1.21.x
server or a Velocity proxy.

> The jar bundles the database drivers (SQLite, PostgreSQL, MariaDB, MongoDB). The MongoDB
> driver is the largest of these; if you only use SQL backends you can drop the
> `mongodb-driver-sync` dependency to keep the jar smaller. The REST API and webhook modules
> add no external dependencies (they use only the JDK).

## Configuration

On first start a `config.yml` is created in the plugin's data folder (written automatically
on Paper, copied from the jar on Velocity). It selects the storage backend, holds credentials,
and configures the REST API and webhooks.

### Storage

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
    connection-string: ""   # takes precedence over the discrete fields if set
    host: "localhost"
    port: 27017
    database: "serverdoctor"
    username: "serverdoctor"
    password: "changeme"
    auth-database: "admin"
```

If the configured backend cannot be reached at startup, ServerDoctor falls back to SQLite
and then to In-Memory rather than blocking the server from starting. On a network, point the
proxy and every backend server at the same PostgreSQL/MariaDB/MongoDB for a shared history.

### REST API

```yaml
rest-api:
  enabled: false
  host: "127.0.0.1"   # use 0.0.0.0 to expose it — then set a token
  port: 9173
  token: ""           # if set, all endpoints except /health require Bearer auth
```

Endpoints (all GET, read-only):

| Path | Auth | Returns |
|---|---|---|
| `/health` | no | `{status, name, version}` |
| `/performance` | token | latest TPS/MSPT/memory snapshot |
| `/conflicts` | token | detected conflicts |
| `/security` | token | security / maintenance risks |
| `/recommendations` | token | recommendations |
| `/report` | token | latest full report (404 if none yet) |

```bash
curl -H "Authorization: Bearer <token>" http://127.0.0.1:9173/report
```

### Webhooks

```yaml
webhooks:
  enabled: false
  min-severity: HIGH    # INFO | LOW | MEDIUM | HIGH | CRITICAL
  targets:
    - type: discord     # discord | slack | teams
      name: "ops"
      url: "https://discord.com/api/webhooks/…"
```

Webhooks fire only on a **status change** (new / worse / recovered) at or above
`min-severity` — not on every background scan — so your channels don't get spammed.

> Note: Microsoft is deprecating Office-365-connector webhooks for Teams. For new Teams
> setups, use a Power Automate workflow webhook (it also accepts JSON POSTs).

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
- `serverdoctor-example-plugin` (reference integration)
- A real security advisory source (currently a metadata heuristic, no invented CVE database)
