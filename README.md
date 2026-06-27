# ServerDoctor

[![Build](https://github.com/Shvquu/server-doctor/actions/workflows/workflow.yml/badge.svg)](https://github.com/Shvquu/server-doctor/actions/workflows/workflow.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-success.svg)](https://github.com/Shvquu/server-doctor/releases)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper/Folia](https://img.shields.io/badge/Paper%2FFolia-1.21.x-brightgreen.svg)](https://papermc.io/)
[![Velocity](https://img.shields.io/badge/Velocity-3.5-brightgreen.svg)](https://papermc.io/software/velocity)
[![BungeeCord](https://img.shields.io/badge/BungeeCord-1.21-brightgreen.svg)](https://github.com/SpigotMC/BungeeCord)

Read-only analysis, diagnostics and monitoring platform for Minecraft servers and proxies.
**Analyzes, evaluates, recommends, warns — never changes anything on the server.**

ServerDoctor runs the same engine on **Paper/Folia**, **Velocity** and **BungeeCord**, shipped as
a single jar. The core is platform-neutral; thin adapters wire it into each platform. A strict
architecture boundary (enforced as a build breaker) guarantees the analysis layer can never reach
into a platform SDK and the platform adapters stay read-only.

> **v1.0.0** — first stable release: 11 read-only scanners, five storage backends, cross-node
> consistency checks, a Prometheus endpoint, report export and baselines. No fabricated data, no
> silent fallbacks — honest, actionable diagnostics.

## Modules

| Module                    | Contents                                                                                | Status    |
|---------------------------|-----------------------------------------------------------------------------------------|-----------|
| `serverdoctor-common`     | Domain models, utilities, exception base                                                | verified  |
| `serverdoctor-api`        | Public contract, events, module SPI                                                     | verified  |
| `serverdoctor-core`       | Engine, 8 scanners, recommendations, conflict DB, update checker, optional sources      | verified  |
| `serverdoctor-storage`    | StorageProvider, 5 repositories — In-Memory, SQLite, PostgreSQL, MariaDB, MongoDB       | verified  |
| `serverdoctor-rest-api`   | Read-only HTTP/JSON endpoints (JDK only)                                                | buildable |
| `serverdoctor-webhook`    | Discord / Slack / Teams notifications (JDK only)                                        | buildable |
| `serverdoctor-testing`    | Fake-platform fixtures, JUnit 5 suite, ArchUnit rules                                   | verified  |
| `serverdoctor-paper`      | Bukkit/Paper/Folia adapter, in-game GUI, command, PlaceholderAPI bridge, storage wiring | buildable |
| `serverdoctor-velocity`   | Velocity adapter, command, storage wiring                                               | buildable |
| `serverdoctor-bungeecord` | BungeeCord adapter, command, storage wiring                                             | buildable |
| `serverdoctor-universal`  | Bundles Paper + Velocity + BungeeCord into one shaded jar                               | buildable |

## Scanners

Eleven read-only scanners run every analysis (proxy platforms automatically skip the ones that
need a tick loop or a world):

- **Plugin** — inventory and metadata of installed plugins.
- **Dependency** — missing or soft dependencies.
- **Conflict** — known plugin-vs-plugin conflicts (conflict database).
- **Performance** — TPS / MSPT / RAM thresholds (Paper/Folia).
- **Security** — maintenance/metadata risks, plus optional advisory lookups (see below).
- **Compatibility** — declared `api-version` vs. server version, Folia support, enabled state,
  aggregated into a 0–100 risk score.
- **Regression** — compares older vs. newer stored snapshots to catch gradual TPS/MSPT/RAM
  decline over time.
- **Configuration** — reviews `server.properties`, `bukkit.yml`, `spigot.yml`,
  `paper-global.yml`, `paper-world.yml` and `velocity.toml` for settings that commonly hurt
  performance or safety, and recommends fixes.
- **Disk** — free space on the data directory's filesystem and the size of the log directory.
- **Runtime** — JVM and Java version vs. the recommended JDK 21, max heap vs. system RAM, and GC
  flags. Self-probing; needs no wiring.
- **Cross-node** — compares server, Java and plugin versions across every node writing to the same
  shared database and flags version drift (see *Cross-node* below).

### Honest by design
Three scanners can be enriched by **real, external feeds you (or the community) maintain** —
ServerDoctor never invents data:

- **Security advisories** (`security.advisory`): there is no canonical CVE database for Minecraft
  plugins, so advisory data comes from a feed at a URL you configure. Off by default.
- **Compatibility metadata** (`compatibility.metadata`): release age, a Folia flag and known
  incompatibilities come from an optional feed; the runtime checks (api-version, Folia, enabled)
  work without it.
- **Configuration** recommendations are widely-accepted best practices and only fire when the key
  is actually present, so a changed/absent key produces nothing rather than bad advice.

## Building

Requirements: JDK 21, Gradle 9.0+ (the Shadow plugin requires it — or run `./gradlew`, the
wrapper is included).

```bash
gradle :serverdoctor-universal:shadowJar
```

Output: `serverdoctor-universal/build/libs/serverdoctor-1.0.0.jar`

That one jar carries a `plugin.yml` (Paper/Folia), a `velocity-plugin.json` (Velocity) **and** a
`bungee.yml` (BungeeCord), so the same file drops into the `plugins/` folder of a Paper 1.21.x
server, a Velocity proxy or a BungeeCord proxy.

> The jar bundles the database drivers (SQLite, PostgreSQL, MariaDB, MongoDB). The MongoDB driver
> is the largest; if you only use SQL backends you can drop the `mongodb-driver-sync` dependency
> to keep the jar smaller.

## Configuration

On first start a `config.yml` is created in the plugin's data folder (written automatically on
Paper, copied from the jar on Velocity/BungeeCord). It selects the storage backend and holds the
credentials for the server-based backends.

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

If the configured backend cannot be reached at startup, ServerDoctor falls back to SQLite and
then to In-Memory rather than blocking the server from starting. On a network, a shared
PostgreSQL/MariaDB/MongoDB is the point: the proxy and the backend servers write into the same
database. SQLite and In-Memory are best for a single node.

### Cross-node

The **cross-node** scanner compares each node's server/Java/plugin versions against the others to
catch a backend that's been forgotten on an old build. It only sees other nodes through a
**shared** database, so it stays quietly inactive on SQLite or In-Memory (single node by design).
To enable real cross-node checks, point every node at the same PostgreSQL/MariaDB/MongoDB instance
and give each one a unique name:

```yaml
network:
  # Unique identifier for this node in cross-node checks.
  # If empty, falls back to "<platform>-<port>" (e.g. paper-25565, velocity-25577).
  node-name: ""
```

Each node upserts its own fingerprint after every scan; the scanner reads the others.

### Optional features

```yaml
# In-game GUI (Paper/Folia)
gui:
  enabled: true
  title: "ServerDoctor"

# Automated periodic scan (Paper/Folia)
tasks:
  scan:
    enabled: true
    interval-seconds: 120
    initial-delay-seconds: 30
    warn-on-high: true

# Read-only HTTP/JSON API (binds to 127.0.0.1 by default)
rest-api:
  enabled: false
  host: "127.0.0.1"
  port: 9173
  token: ""              # if set, all endpoints except /health require Bearer auth

# Outbound notifications (fire on status change, not every scan)
webhooks:
  enabled: false
  min-severity: HIGH
  targets:
    - type: discord      # discord | slack | teams
      url: ""
      name: "ops"
  # Periodic summary pushed to the same targets, regardless of status change.
  digest:
    enabled: false
    interval-minutes: 1440

# Optional external feeds (off by default — see "Honest by design")
security:
  advisory:
    enabled: false
    feed-url: ""
    refresh-minutes: 360

compatibility:
  metadata:
    enabled: false
    feed-url: ""
    refresh-minutes: 1440
```

## In-game GUI (Paper/Folia)

`/serverdoctor gui` opens a read-only inventory menu: a status overview plus screens for
Performance, Conflicts, Security, Recommendations and History, with a Refresh button that re-runs
the analysis. It's Folia-safe (entity/async schedulers) and never modifies anything.

## Operator tooling

- **Report export** — `/serverdoctor export [json|md|html]` writes the latest report to
  `<data folder>/exports/`. Handy for tickets, audits or sharing a snapshot.
- **Baselines** — `/serverdoctor baseline pin` stores the current report as a known-good baseline
  in `<data folder>/baseline.properties`; `/serverdoctor baseline` then prints exactly what has
  drifted since (TPS, MSPT, RAM, conflict/risk counts).
- **Health digest** — an optional periodic summary pushed to your webhook targets (see
  `webhooks.digest`), independent of the status-change notifications.
- **Prometheus** — when the REST API is enabled, `/metrics` exposes the latest snapshot in
  Prometheus text format for scraping (token-gated like the other endpoints).

## Commands

```
/serverdoctor scan                    # run a full analysis
/serverdoctor report                  # show the latest report
/serverdoctor tps                     # live performance (TPS/MSPT/RAM)
/serverdoctor conflicts               # detected plugin conflicts
/serverdoctor security                # security and maintenance risks
/serverdoctor recs                    # generated recommendations
/serverdoctor history                 # stored performance history (Paper/Folia)
/serverdoctor gui                     # open the in-game GUI (Paper/Folia)
/serverdoctor export [json|md|html]   # export the latest report to disk
/serverdoctor baseline [pin]          # pin a baseline / show drift since it
/serverdoctor reload                  # reload messages.yml
```

Aliases: `/sd`, `/doctor` · Permission: `serverdoctor.admin` (default: op).
A configurable background scan also runs automatically (every 120 s by default).

## REST API

Enable `rest-api` in `config.yml`. All endpoints are GET and return JSON (except `/metrics`,
which returns Prometheus text):

| Endpoint           | Description                  | Auth           |
|--------------------|------------------------------|----------------|
| `/health`          | liveness probe               | none           |
| `/performance`     | latest TPS/MSPT/RAM snapshot | token (if set) |
| `/conflicts`       | detected conflicts           | token (if set) |
| `/security`        | security/advisory risks      | token (if set) |
| `/recommendations` | recommendations              | token (if set) |
| `/report`          | full latest report           | token (if set) |

## Webhooks

`webhooks` supports **Discord**, **Slack** and **Microsoft Teams**. Notifications fire only on a
status change at or above `min-severity` (new / worse / recovered), so the background scan doesn't
spam your channel. A separate **health digest** (`webhooks.digest`) can push a periodic summary on
a fixed interval.

## Tests

Unit tests (JUnit 5) live in `src/test` of the respective modules; fixtures and the ArchUnit
architecture tests are in `serverdoctor-testing`.

```bash
gradle test
```

Coverage includes version/severity logic, the AnalysisResult builder, ScannerRegistry capability
gating, the EventBus (including error isolation), every scanner's thresholds (incl. regression,
configuration, disk, runtime and cross-node), the RecommendationEngine, the analysis engine
end-to-end, report export and baseline diffing, the Prometheus formatter, and storage round-trips.
The ArchUnit rules enforce, as build breakers: no platform SDK in Core/Common/API/Storage, the
Clean Architecture dependency rule, and the read-only invariant of the platform adapters.

> Note: JUnit must stay on the 5.x line — ArchUnit's JUnit 5 integration does not yet support
> JUnit Platform 6.

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

API failures share a common base, so you can catch them in one place: `ServerDoctorException`
(with `ApiNotInitializedException`, `ConfigurationException`, `StorageException`,
`AnalysisException`).

## Roadmap

- `serverdoctor-example-plugin` (reference integration).
- Trend detection for finding/conflict counts at the persistence layer (the regression scanner
  currently covers TPS/MSPT/RAM).

---

ServerDoctor is read-only by design. It observes, measures and advises — it never modifies your
server. Full documentation lives in the [Wiki](https://github.com/Shvquu/server-doctor/wiki).
