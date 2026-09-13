# Phase 2: usage events and derived totals

## Scope

Record manual usage for any generic item, show its chronological history, edit or delete an event, and derive total usage and cost per unit from source data. The workflow supports distance, hours, wears, washes, uses and custom metrics without category-specific models.

Photos, the dashboard and automatic distance collection remain later phases.

## Persistence and calculations

- Room schema version 2 adds `usage_events` with an `Item 1:N UsageEvent` foreign key and cascade cleanup if item deletion is introduced later.
- `MIGRATION_1_2` creates the table and index without replacing the existing database; migration coverage verifies that version 1 items and metrics survive.
- Usage values are stored as normalized decimal strings, avoiding binary floating-point drift. Input accepts a point or comma, requires a positive value, and supports up to 6 fractional digits and 18 significant digits.
- Event dates cannot be in the future. Notes allow up to 4000 characters.
- Every current event is `MANUAL`; the domain also defines `AUTOMATIC` so a later Android data source can use the same model.
- Total usage is summed from events and is never persisted as a second source of truth.
- Cost per unit is derived from the item's exact minor-unit price and total usage, rounded half-up to the currency's standard fraction digits. It is unavailable when price or usage is absent.
- Counter metrics provide a convenient `+1` action while retaining the generic decimal event contract.

## UI

- Item cards show total usage and cost per unit when available.
- Item details prioritize total usage, cost per unit and event count.
- Usage history is ordered newest date first, then creation order.
- Tapping a history entry opens editing; deletion requires confirmation and immediately recalculates totals.
- Editor state survives activity recreation through Compose saveable state, and repeated writes are guarded while storage work is in progress.

## Verification

Run `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` with the Android Studio JBR and an emulator.

Tests cover exact decimal parsing, invalid inputs, item-scoped totals, cost rounding, event create/edit/delete, version 1 data migration, navigation arguments, and the Compose flow `5 km + 7 km = 12 km` with `$100 / 12 = $8.33/km` followed by edit and delete recalculation.

## Local acceptance results (2026-09-12)

- `testDebugUnitTest`: 11 passed.
- `lintDebug`: passed; existing dependency and Gradle deprecation warnings remain informational.
- `assembleDebug`: passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `connectedDebugAndroidTest`: 7 passed on Medium_Phone_API_36.1 (Android 16).
- API 26 coverage remains configured in GitHub Actions; physical-device acceptance is not claimed.
- Physical-device acceptance is not claimed.
