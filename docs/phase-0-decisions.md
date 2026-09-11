# Phase 0 decisions

## Product contract

StuffStats tracks generic physical items. Categories organize items but do not determine behavior. Usage is modeled through metrics and events rather than category-specific entities such as `Shoe` or `TShirt`.

## Planned data model

The first Room schema will use four main tables:

```text
items
item_metrics
usage_events
item_photos
```

The MVP UI exposes one primary metric per item, while the separate `item_metrics` table leaves a direct path to multiple metrics later.

Derived values such as total usage and cost per unit are calculated from source data and are not persisted.

## Android baseline

- Package: `com.aleksandrantonikov.stuffstats`
- Minimum Android version: API 26
- Compile and target API: 36
- Single `app` Gradle module until project size justifies extraction
- UI: Jetpack Compose and Material 3
- State: immutable UI state exposed from ViewModels through StateFlow
- Persistence planned for Phase 1: Room behind repositories
- Navigation: Navigation Compose

## Privacy

Android cloud backup and device-to-device transfer are disabled for application data. No Internet permission is declared. No analytics or remote SDK is included.

Photos will be copied into app-private storage rather than stored as blobs in Room. Room will keep only paths and metadata.

## Time and numeric representation

- Calendar dates will be stored independently from time zones.
- Creation timestamps will use instants.
- Money will be stored in minor currency units plus an ISO currency code, not floating point.
- Usage values may be fractional and must be finite and positive.

## Quality gates

Every feature phase should preserve these checks:

```text
testDebugUnitTest
lintDebug
assembleDebug
```

Room schema export and migration tests will become mandatory when Room is introduced.

## Deferred decisions

- public license;
- supported currencies and the default currency;
- whether version 1 ships in English, Russian, or both;
- Play Store distribution and signing ownership;
- manual encrypted export/import after MVP.
