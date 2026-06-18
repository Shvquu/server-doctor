# Changelog
## [Unreleased]

_Planned: BungeeCord adapter, an example integration plugin, and a real security advisory
source._

## [0.9.0](https://github.com/Shvquu/server-doctor/releases/tag/v0.9.0) - 2026-06-18

### Added
- BungeeCord support: new `serverdoctor-bungeecord` proxy adapter (HAS_PLUGINS + IS_PROXY).
  The same universal jar now runs on Paper/Folia, Velocity and BungeeCord (plugin.yml +
  velocity-plugin.json + bungee.yml side by side), with full storage-backend selection.
- In-game GUI (`/serverdoctor gui`) for Paper/Folia: status overview plus Performance,
  Conflicts, Security, Recommendations and History screens, with a Refresh button.
  Folia-safe; read-only.
- Configurable automated scan (Paper/Folia) via `tasks.scan`
  (`enabled`, `interval-seconds`, `initial-delay-seconds`, `warn-on-high`).
- Security advisory source (all platforms, off by default): `AdvisorySource` +
  `RemoteAdvisorySource` checking installed plugins against a real, external feed
  (`security.advisory`). Never invents advisories.
- `gui` and `security.advisory` config sections.

### Changed
- REST API and webhooks are now available on BungeeCord too (previously Paper/Velocity only);
  all three platform mains wire storage, advisory, REST and webhooks consistently.
- The hard-coded 5-minute background scan (Paper) is now config-driven.
- `ServerDoctorCore.bootstrap` gained an `AdvisorySource` overload; `bootstrap(platform)` is
  unchanged.

### Fixed
- BungeeCord now writes its default `config.yml` and `messages.yml` on first start
  (class-relative resource loading).
- Configurable scan interval clamped to a sane minimum (10 s).

## [0.8.1 - 0c07127](https://github.com/Shvquu/server-doctor/releases/tag/0c07127) - 2026-17-06

### Added
- **In-game GUI** — `/serverdoctor gui` (alias `/sd gui`): a read-only menu with a status
  overview plus screens for Performance, Conflicts, Security, Recommendations and History, and
  a Refresh button that re-runs the analysis. Paper/Folia only.
- **Folia-safe GUI** — screens open and rebuild on the player's entity scheduler; Refresh runs
  the analysis on the async scheduler. Clicks are routed via an `InventoryHolder` and always
  cancelled (nothing on the server is ever modified).
- **Configurable automated scan** in `config.yml` under `tasks.scan` (`enabled`,
  `interval-seconds`, `initial-delay-seconds`, `warn-on-high`) — e.g. every 120 seconds.
- **`gui` config section** (`enabled`, `title`).

### Changed
- The previously hard-coded 5-minute background scan is now driven by `config.yml`. Defaults
  preserve the old behaviour.

### Fixed
- The scan interval is clamped to a sane minimum (10 s) so a misconfiguration can't hammer the
  server.
- Various stability and performance improvements.

## [0.8.0 - c86119d](https://github.com/Shvquu/server-doctor/releases/tag/v0.8.0) - 2026-06-16

### Added
- **`serverdoctor-rest-api`** — a read-only HTTP/JSON API built on the JDK's
  `com.sun.net.httpserver` (no external dependencies). Endpoints (all GET): `/health`
  (no auth), `/performance`, `/conflicts`, `/security`, `/recommendations`, `/report`.
- **Bearer-token auth** for the REST API: when `rest-api.token` is set, every endpoint except
  `/health` requires `Authorization: Bearer <token>`. Binds to `127.0.0.1` by default.
- **`serverdoctor-webhook`** — outbound notifications to **Discord**, **Slack** and
  **Microsoft Teams** via the JDK `HttpClient` (no external dependencies).
- **Change-based webhook delivery** — alerts fire only on a status change (new / worse /
  recovered) at or above a configurable `min-severity`, instead of on every scan, so the
  5-minute background scan no longer produces repeated notifications.
- **`rest-api` and `webhooks` sections** in `config.yml`, read on both Paper/Folia and Velocity.

### Changed
- Both services are fully integrated into the Paper/Folia and Velocity adapters and bundled
  into the universal jar; they remain framework-free and depend only on the public API and
  common modules.

### Fixed
- Webhook notifications no longer repeat unchanged alerts on each background scan.
- REST server threads run as daemons and shut down cleanly with the plugin.
- Various stability and performance improvements.

## [0.7.0 - 04adb6b](https://github.com/Shvquu/server-doctor/releases/tag/v0.7.0) - 2026-06-15

### Added
- **Pluggable storage backends** — **PostgreSQL**, **MariaDB** and **MongoDB**, alongside the
  existing **SQLite** and **In-Memory** options, all selectable at runtime.
- **`config.yml`** for choosing the storage backend and entering credentials. It is created
  automatically on first start (written by Bukkit on Paper/Folia, copied from the jar on
  Velocity) and never overwrites an existing file.
- **HikariCP connection pooling** for the SQL backends, with a small dialect abstraction so
  PostgreSQL and MariaDB share one provider and differ only where the SQL dialect does.
- **MongoDB** support via either discrete `host/port/credentials/auth-database` fields or a
  full connection string (e.g. an Atlas SRV URI).
- **Velocity config parsing** (SnakeYAML), so the proxy reads the same `config.yml` shape.

### Changed
- Storage is fully wired into both the Paper/Folia and Velocity adapters; the persistence
  layer stays platform-free, with the adapters translating config into a neutral
  `StorageConfig`.

### Fixed
- Graceful startup fallback: if the configured backend is unreachable, ServerDoctor falls
  back to SQLite and then In-Memory instead of blocking server start.
- Indexed timestamp columns use `VARCHAR(64)` to satisfy MariaDB's index rules (it cannot
  index `TEXT` without a prefix length).
- Connection pools are closed cleanly on shutdown.
- Various stability and performance improvements.

## [0.6.0](https://github.com/Shvquu/server-doctor/releases/tag/v0.6.0) - 2026-06-14

_Multi-platform foundation (state at this release)._

### Added
- **Velocity adapter** and a **`serverdoctor-universal`** module that bundles Paper and
  Velocity into a single jar (one file carrying both `plugin.yml` and `velocity-plugin.json`).
- **Folia support** via an alternate scheduler adapter selected at runtime.
- **PlaceholderAPI** expansion on Paper/Folia.
- **Update checker** against GitHub releases.
- **`messages.yml`** with reloadable, customizable output (`/serverdoctor reload`).

### Notes
- Storage at this point is SQLite + In-Memory; the five scanners (Plugin, Dependency,
  Conflict, Performance, Security), the recommendation engine, the public API and the event
  bus are all in place.

## [0.5.0](https://github.com/Shvquu/server-doctor/releases/tag/v0.5.0) - 2026-06-13

_Initial runnable foundation._

### Added
- Four-module core (`common`, `api`, `core`, `storage`) plus a buildable Paper plugin.
- Five scanners (Plugin, Dependency, Conflict, Performance, Security), the recommendation
  engine and a conflict database.
- Public API with an event system and a module SPI for third-party scanners.
- In-game `/serverdoctor` command (aliases `/sd`, `/doctor`).
- SQLite and In-Memory storage.
- JUnit 5 test suite and ArchUnit architecture rules enforcing, as build breakers: no platform
  SDK in core/common/api/storage, the Clean Architecture dependency rule, and the read-only
  invariant of the platform adapters.

[Unreleased]: https://github.com/Shvquu/server-doctor/compare/v0.9.0...HEAD
[0.9.0 - b858d8d]: https://github.com/Shvquu/server-doctor/releases/tag/v0.9.0
[0.8.1 - 0c07127]: https://github.com/Shvquu/server-doctor/releases/tag/0c07127
[0.8.0 - c86119d]: https://github.com/Shvquu/server-doctor/releases/tag/v0.8.0
[0.7.0 - 04adb6b]: https://github.com/Shvquu/server-doctor/releases/tag/v0.7.0
[0.6.0]: https://github.com/Shvquu/server-doctor/releases/tag/v0.6.0
[0.5.0]: https://github.com/Shvquu/server-doctor/releases/tag/v0.5.0