# serverdoctor-example-plugin

A minimal, standalone **reference integration** for the ServerDoctor API. It's a separate Paper
plugin (not part of the shaded ServerDoctor jar) that shows how a third-party plugin consumes
ServerDoctor — read-only, the way the API is meant to be used.

## What it demonstrates

1. **Obtaining the API** via `ServerDoctorProvider.get()` (with `depend: [ServerDoctor]` in
   `plugin.yml` so load order is guaranteed, plus a graceful fallback if it's missing).
2. **Reading live diagnostics**: `getServerInfo()`, `getPlugins()`, `getRegisteredModuleIds()`,
   `getOverallSeverity()`, `getLastRunTimestamp()`, `getConflicts()/getSecurityRisks()/getRecommendations()`.
3. **Subscribing to events** — including the 1.0.0 additions
   (`OverallSeverityChangedEvent`, `ScannerFailedEvent`, `RecommendationGeneratedEvent`) alongside
   the existing conflict/risk/finished/performance events.
4. **Registering a custom scanner** (`ExampleScanner`) so your own findings join every run.
5. A `/sdexample` command that prints a summary and can trigger a fresh `runDiagnostics()`.

## Files

- `ExampleScanner.java` — a tiny `AnalysisModule` (reports the installed-plugin count).
- `ServerDoctorExamplePlugin.java` — the plugin: wiring, events, command.
- `plugin.yml` — `depend: [ServerDoctor]`, the `sdexample` command + permission.
- `build.gradle.kts` — `compileOnly` on `serverdoctor-api`, `serverdoctor-common` and Paper.

## Build & install

1. Add the module to `settings.gradle.kts`:

   ```kotlin
   include(
       // ...existing modules...
       "serverdoctor-example-plugin",
   )
   ```

2. Build just the example jar:

   ```bash
   gradle :serverdoctor-example-plugin:jar
   ```

   Output: `serverdoctor-example-plugin/build/libs/serverdoctor-example-plugin-<version>.jar`

3. Drop **both** ServerDoctor and this jar into `plugins/`. ServerDoctor loads first (because of
   `depend`), then the example plugin connects to its API.

> It depends on the API only at compile time (`compileOnly`); at runtime the classes come from the
> installed ServerDoctor plugin, so the example jar stays tiny and never bundles ServerDoctor.

## Notes

- Everything here is read-only. The example never modifies the server; it only observes
  ServerDoctor and contributes findings through the public SPI.
- `PerformanceThresholdReachedEvent` only fires on Paper/Folia (proxies have no tick loop), but
  subscribing to it is harmless everywhere.
