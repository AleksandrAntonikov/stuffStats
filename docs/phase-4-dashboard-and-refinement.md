# Phase 4: dashboard and UI refinement

## Scope

Make the local data accumulated in Phases 1–3 easier to find and interpret. This phase adds catalog search, category and archive filters, deterministic sorting, lifetime usage statistics and a cross-item dashboard.

No new Room tables are required. Automatic distance collection, health integrations, sync and remote services remain outside this phase.

## Item statistics

- Total usage and cost per unit remain derived from source events.
- First and last usage dates come from the earliest and latest matching events.
- Average weekly and monthly usage use the inclusive period from the first recorded event through today. The month length is the Gregorian average of 30.4375 days; displayed results are rounded half-up to two decimals.
- Days owned use the inclusive period from purchase date through today and remain unavailable when no purchase date is recorded.
- Item details show event and photo counts alongside these lifetime values.

## Dashboard semantics

- Active and archived item counts are displayed separately.
- Event, photo and purchase totals include active items only.
- Purchase totals are grouped by ISO currency; unlike currencies are never added together.
- “Most regularly tracked” means the active item with the largest number of source usage events. This avoids comparing unrelated quantities such as kilometers, hours and washes.
- Cost leaders are selected separately for each currency, metric type and case-insensitive unit. A USD/km value is therefore never ranked against EUR/km or USD/hour.
- Every item insight links back to its item details.

## Catalog and UI

- Search covers item name, notes, category, metric type and unit.
- Category filtering composes with the existing Active/Archive switch.
- Sorting supports recently added, name and last usage date.
- Query, category, archive selection and sort survive activity recreation through saveable Compose state.
- Item cards emphasize the current total, cost per unit, latest-use date and newest condition photo.
- English and Russian resources cover all new controls and statistics.

## Verification

Run `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` with the Android Studio JBR and an emulator.

Unit tests cover date ranges, averages, mixed-currency totals, comparable cost leaders, active-only dashboard aggregation and combined catalog filtering. The Compose workflow verifies dashboard navigation and search-state restoration.

### Local acceptance — 2026-09-13

- The combined Gradle gate completed successfully with 18 unit tests and 11 instrumentation tests.
- Android lint completed with no reported issues and the debug APK assembled successfully.
- Instrumentation ran on `Medium_Phone_API_36.1`, Android API 36, and covered creation, editing, archive/restore, usage, photos, dashboard navigation and recreation-safe search.
- A runtime smoke check of the installed APK confirmed the dashboard renders its overview, active/archive counts, usage and photo counts, purchase totals, most-tracked insight and cost-leader sections, including empty states.
- A physical-device pass was not performed; no hardware-dependent behavior was introduced in this phase.
