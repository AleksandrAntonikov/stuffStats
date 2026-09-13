# Phase 3: condition photos

## Scope

Attach a camera or gallery image to any generic item, preserve it in app-private storage, and show a chronological condition history. Every photo records the item's exact accumulated usage at the moment the user saves it.

Dashboard statistics, filtering refinements, photo comparison and automatic distance collection remain later phases.

## Persistence and privacy

- Room schema version 3 adds `item_photos` with an `Item 1:N ItemPhoto` foreign key.
- `MIGRATION_2_3` creates the table and index without replacing the database; migration coverage verifies that version 2 items and usage events survive.
- A photo stores a private relative file name, calendar date, normalized decimal usage snapshot, optional notes and creation timestamp. Image bytes are not stored in Room.
- Selected and captured images are decoded, EXIF-oriented, scaled to a maximum 2048-pixel edge and re-encoded as JPEG in `files/item_photos`.
- Import is bounded to 40 MiB. A failed database write removes the newly imported file, and deleting a photo also removes its private file.
- The system Photo Picker requires no storage permission. Camera capture uses a temporary, non-exported `FileProvider` URI and does not add Internet or broad media permissions.
- Android backup and device-to-device transfer remain disabled, so photos and database records stay local to the device.

## UI

- Item cards and details show the newest condition photo when available.
- Item details expose direct actions to add a photo or open the complete history.
- The add-photo screen supports the system picker and installed camera app, date and notes, and guards repeated saves.
- Photo history is newest first and displays the immutable usage snapshot, metric unit, date and notes.
- Deletion requires confirmation and removes both the history row and private image.

## Verification

Run `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` with the Android Studio JBR and an emulator.

Tests cover photo validation, private image normalization and cleanup, Room create/delete behavior, exact decimal snapshots, version 2 migration, and photo navigation routes. External camera and picker UI are intentionally exercised manually because they are provided by another application.

## Local acceptance results (2026-09-13)

- `testDebugUnitTest`: 14 passed.
- `lintDebug`: no errors; 11 dependency-update warnings remain informational.
- `assembleDebug`: passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `connectedDebugAndroidTest`: 10 passed on Medium_Phone_API_36.1 (Android 16).
- The system Photo Picker opened without a broad media permission; a selected image was previewed, saved and shown with its `0 km` snapshot in photo history.
- The emulator camera received the temporary URI and returned a captured preview; backing out removed the temporary file.
- After a force-stop and cold start, the saved photo remained visible in the item card and details.
- The merged manifest has no Internet, camera or broad media permission; backup remains disabled and the photo `FileProvider` is not exported.
- API 26 coverage remains configured in GitHub Actions but has not run remotely for this phase. Physical-device camera acceptance is not claimed.

## Local acceptance results (2026-09-13)

- `testDebugUnitTest`: 14 passed.
- `lintDebug`: no errors; dependency-update warnings remain informational.
- `assembleDebug`: passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `connectedDebugAndroidTest`: 10 passed on Medium_Phone_API_36.1 (Android 16).
- Manually opened the system Photo Picker, selected an image, saved it with a `0 km` snapshot and verified the photo-history entry.
- Manually opened the system camera, captured an image, returned to the in-app preview and verified cancellation cleans its temporary file.
- Force-stopped and reopened the app; the saved photo and home-card thumbnail remained available.
- The merged manifest has no Internet, camera or broad media permission; backup remains disabled and the photo `FileProvider` is not exported.
- API 26 coverage remains configured in GitHub Actions but has not run remotely for this phase. Physical-device camera acceptance is not claimed.
