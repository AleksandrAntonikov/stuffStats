# Phase 5: automatic distance import

## Scope

Phase 5 adds Health Connect as an optional automatic source for items whose primary metric is `DISTANCE`. The user chooses both the item and calendar date before any data is read. Manual usage entry remains available on every supported Android version and does not depend on Health Connect.

This phase does not add background collection, step-to-distance estimation, location access, Internet access, accounts, synchronization, or a new Room schema.

## Data flow

1. The item details screen offers Health Connect import only for active distance items.
2. The import screen checks provider availability and the read-distance permission.
3. After explicit permission, Health Connect aggregates the selected date's distance while accounting for overlapping provider records.
4. The aggregate is converted from meters to the item's `m`, `km`, or `mi` unit with at most six decimal places.
5. StuffStats stores the value through the existing `UsageEventRepository` with `source = AUTOMATIC`.

An import is keyed by item and date. Repeating it updates the existing automatic event instead of adding another one, so a corrected Health Connect aggregate does not double count. Manual events on the same date are independent and are never overwritten.

## Privacy and availability

- The manifest requests only `android.permission.health.READ_DISTANCE`.
- Permission is requested from the import screen, not at application launch.
- A dedicated rationale activity explains the exact read scope and local storage behavior.
- Imported values remain in the local Room database; StuffStats still has no Internet permission.
- When Health Connect is unavailable, manual tracking continues unchanged. When its provider needs installation or an update, the app offers the official provider-store flow.

## Deliberate limits

- Daily distance can only be attributed to the item the user explicitly selects; StuffStats does not guess which shoes or other item were in use.
- Only recorded distance is read. Steps are not converted because stride estimation would introduce unverified data.
- Import runs in the foreground. Background health permission and periodic work are outside this phase.
- Health Connect may restrict how far back another app can read data, depending on when permission was first granted.

## Verification

Run `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` with the Android Studio JBR and an emulator. Unit coverage verifies unit conversion and idempotent event replacement. Compose coverage verifies navigation to the import screen, date restoration and the privacy rationale UI.

### Local acceptance — 2026-09-13

- The combined Gradle gate completed successfully with 23 unit tests and 14 instrumentation tests.
- Android lint reported no issues and the debug APK assembled successfully.
- Instrumentation ran on `Medium_Phone_API_36.1`, Android API 36, including live Health Connect availability and permission checks.
- A runtime smoke test opened the distance import for a real local item, completed the Android Health Connect permission flow with only Distance selected, and performed an aggregate read for the current date. The device had no distance data, and the app correctly displayed its no-data state without creating usage.
- APK inspection confirmed `READ_DISTANCE` is the only health-data permission. No Internet, background-health, or full-history permission is present.
- A populated Health Connect source and physical-device behavior remain unverified because the available emulator contained no distance records.
