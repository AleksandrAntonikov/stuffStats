# Phase 1: local item management

## Scope

Create, list, inspect, edit, archive and restore generic items. Each item has one primary metric, an optional purchase price and date, a category, and notes. English and Russian UI resources are included.

Usage recording, photos, derived statistics and the dashboard remain later phases. The first schema includes only `items` and `item_metrics`; event and photo tables will be introduced with their features and explicit migrations.

## Persistence and validation

- Room 2.8.5, KSP, schema version 1 exported under `app/schemas`.
- Serialization BOM 1.8.1 aligns the application and Room migration-test runtime; the older Navigation transitive version caused an AbstractMethodError during schema testing.
- Repositories isolate Room from the ViewModel and UI.
- Item and metric writes are transactional. Editing preserves identity, creation time and archive state.
- Metrics use separate rows; the MVP repository enforces exactly one per item.
- Archive is reversible and never deletes user data.
- Price is nullable (unknown differs from zero), stored in minor units with an ISO currency code. USD is the editable initial default; currencies with supported nonnegative fraction digits are accepted.
- Decimal point and comma input are accepted without floating-point conversion. Negative, overflowing and over-precision amounts are rejected.
- Purchase date is optional, stored as an epoch day and must not be in the future.
- Names are required (up to 120 characters), units are required (up to 24), notes allow up to 4000.
- Compose saveable state preserves editor fields across activity recreation and Android saved-state restoration. Writes are guarded against repeated taps.
- No destructive migration fallback, Internet permission, cloud backup, account or telemetry.

## Verification

Run `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` with the Android Studio JBR and an emulator.

Tests cover exact currency conversion, invalid values, generic custom metrics, database reopen, edits, archive/restore, rejected writes, exported v1 schema compatibility, and a complete Compose item workflow with activity recreation.

There is no older persisted schema to migrate from: Phase 0 did not store product data. Future schema changes must retain v1 and add migration tests.

## Local acceptance results (2026-09-12)

- `testDebugUnitTest`: 6 passed.
- `connectedDebugAndroidTest`: 5 passed on Medium_Phone_API_36.1 (Android 16).
- `lintDebug`: no errors; dependency-update warnings remain.
- `assembleDebug`: passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- Visually inspected the item editor and populated home screen.
- Manually created Shoes with a USD 100.00 price and distance metric, force-stopped the app, and verified the same item, price and metric after a cold start.
- Merged debug manifest has no Internet permission; backup remains disabled.
- API 26 device coverage is configured in GitHub Actions but has not run remotely in this phase. Physical-device acceptance is not claimed.
