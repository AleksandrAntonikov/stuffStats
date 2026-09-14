# Phase 6: MVP stabilization and release readiness

## Scope

Phase 6 hardens the completed local-first MVP without adding new product capabilities or changing the Room schema. Items, metrics, usage events, condition photos and foreground Health Connect distance import keep their existing behavior.

This phase does not add export/import, background work, Internet access, accounts, analytics, synchronization, extra health permissions or category-specific domain models. Store signing and publication ownership remain product decisions outside the repository.

## Reliability and accessibility

- The catalog uses one vertically scrollable surface so search, filters, sorting, empty states and item cards remain reachable on short screens and with large text.
- Active/archive filters wrap when horizontal space is constrained.
- System Back from an unfinished camera flow follows the same cleanup path as the visible Back control.
- Temporary captures and interrupted photo writes older than 24 hours are pruned on a later app start. Fresh captures are preserved so activity or process recreation during the external camera flow does not discard the pending image.
- Obsolete Phase 0 preview screens and placeholder resources are removed from the production source set.

## Release build

- Release code shrinking, optimization and resource shrinking are enabled with the Android optimized default rules.
- Both debug and release variants are compiled and linted in continuous integration.
- The release artifact remains unsigned until signing ownership and distribution are decided.

## Verification

Run the following local gate with the Android Studio JBR and an emulator:

```text
testDebugUnitTest lintDebug lintRelease assembleDebug assembleRelease connectedDebugAndroidTest
```

The automated suite must cover stale temporary-photo cleanup and catalog reachability at 200% font scale in addition to the existing persistence, migration, usage, photo, dashboard and Health Connect fallback checks.

Manual acceptance should repeat the offline MVP scenario after an application restart, verify camera denial still leaves manual/photo-gallery workflows usable, and inspect light/dark themes with large text. Camera quality and Health Connect behavior with populated data remain physical-device acceptance items.

### Local acceptance — 2026-09-14

- `testDebugUnitTest` passed all 23 unit tests.
- `connectedDebugAndroidTest` passed all 17 instrumentation tests on `Medium_Phone_API_36.1`, Android API 36. This includes catalog reachability at 200% font scale, system-Back photo cleanup and stale-capture pruning.
- `lintDebug`, `lintRelease`, `assembleDebug` and the R8/resource-shrunk `assembleRelease` tasks passed.
- The unsigned release APK decreased from the unoptimized 9,434,545-byte baseline to 1,634,204 bytes.
- APK inspection confirmed that `READ_DISTANCE` remains the only health-data permission and that no Internet permission is present.
- The headless emulator did not provide a usable framebuffer screenshot, so final human visual review and physical-device camera/Health Connect acceptance remain open.
