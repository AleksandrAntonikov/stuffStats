# StuffStats

StuffStats is a local-first Android application for tracking how physical items are used, how they wear over time, and how much each unit of use costs.

Footwear is the first complete use case, but the domain is intentionally generic: an item can accumulate distance, hours, wears, washes, uses, or a custom metric.

## Current status

Phases 0 through 6 are implemented:

- native Android project using Kotlin and Jetpack Compose;
- Material 3 light, dark, and dynamic color themes;
- Navigation Compose routes for the planned MVP screens;
- English and Russian starter resources;
- local-only backup policy;
- unit and instrumentation test foundations;
- GitHub Actions verification.

Items, primary metrics, usage events and condition photos persist locally. You can create, edit, archive and restore items; record usage manually; import a selected day’s distance from Health Connect; build a chronological photo history; search and filter the catalog; and review lifetime and cross-item statistics on the dashboard. Phase 6 adds large-text and compact-screen hardening, temporary-photo cleanup, and optimized release builds. See [Phase 1 details](docs/phase-1-local-items.md), [Phase 2 details](docs/phase-2-usage-events.md), [Phase 3 details](docs/phase-3-condition-photos.md), [Phase 4 details](docs/phase-4-dashboard-and-refinement.md), [Phase 5 details](docs/phase-5-automatic-distance.md) and [Phase 6 details](docs/phase-6-mvp-stabilization.md).

## Technology baseline

- Kotlin 2.2.20
- Android Gradle Plugin 8.11.1
- compileSdk / targetSdk 36
- minSdk 26
- Java/Kotlin target 17
- Jetpack Compose with the Compose BOM
- Material 3
- Navigation Compose

## Build locally

Requirements:

- Android Studio with Android SDK 36;
- JDK 17 or newer. Android Studio's bundled runtime is suitable.

On Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testDebugUnitTest lintDebug lintRelease assembleDebug assembleRelease
```

## Product constraints

- Android only;
- all user data stays local to the device;
- no account, backend, analytics, cloud synchronization, or social features;
- manual usage tracking must remain fully functional even if automatic sources are added later.
- Health Connect access is optional, foreground-only, and limited to reading distance after explicit permission.

See [Phase 0 decisions](docs/phase-0-decisions.md) and the [MVP acceptance scenario](docs/mvp-acceptance-scenario.md).

## Git identity

This repository uses a repository-local identity:

```text
Aleksandr Antonikov <antonikov.alexandr@gmail.com>
```

## License

No license has been selected yet.
