# Inspecta Pocket

Inspecta Pocket is a native Android field-inspection app with an Arabic, right-to-left interface. Inspectors create visits, answer a checklist, record observations, reopen saved visits, and share a structured plain-text report. All inspection data stays in a local Room database; the app works offline and requests no network permission.

## Android baseline and stack

- Android 9 or newer: `minSdk 28`, `compileSdk 36`, `targetSdk 36`.
- Primary acceptance device: OPPO A12 (CPH2083), Android 9, approximately 360 dp wide.
- Kotlin 2.2.10, Jetpack Compose, Material 3, Navigation Compose, ViewModel, Coroutines and Flow.
- Room 2.8.4 with stable checklist IDs and persisted per-visit results.
- JDK 17, Android Gradle Plugin 9.0.1, and the checked-in Gradle 9.1.0 Wrapper.

## MVP workflow

1. Tap **زيارة جديدة**, enter the institution, visit date, and subject, then tap **بدء الزيارة**.
2. Answer the 15 demonstrative checklist items across التنظيم، الوثائق، التجهيز، السلامة، والجانب البيداغوجي. Choose **مطابق**, **غير مطابق**, or **غير منطبق** and optionally add observations.
3. Changes save automatically. Check the save indicator before leaving; use the retry action if saving fails.
4. Return Home to see previous visits and their completion progress. Open a visit to review its summary, results, and notes, or continue editing.
5. Share the visit through Android's share sheet as structured plain text.

Unanswered items remain distinguishable from items marked not applicable. Completion counts answered items, including not-applicable answers; the summary also gives separate counts for each state.

## Build and checks

Install JDK 17 and Android SDK Platform 36, Build Tools 36.0.0, and Platform Tools. A system Gradle installation, Android Studio, and an emulator are not required. The first build needs network access to download Gradle and dependencies; running the app does not.

From the repository root:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Unit tests cover progress and state counts and report transformations. Device tests cover Room persistence and the inspection workflow where implemented. To run instrumented checks with the acceptance phone connected and unlocked:

```bash
./gradlew connectedDebugAndroidTest
```

Device workflow tests may create a clearly labeled acceptance visit in the application's main database. Persistence tests use a separate test database and clean it up. Do not clear the application's user data to prepare tests on a phone containing real inspections.

Machine-specific SDK paths belong in your environment or an untracked `local.properties`. Do not commit SDK paths, signing secrets, build outputs, or IDE caches.

## Install and test on the OPPO A12

Enable USB debugging, connect the phone, unlock it, and authorize this computer when Android prompts. Find the serial with `adb devices -l`, then replace `SERIAL` below:

```bash
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL shell am start -n dz.inspecta.pocket/.MainActivity
```

`install -r` updates the existing debug installation while retaining its data when the signing identity is unchanged.

Human acceptance checklist:

- Create a visit and confirm required-field validation.
- Answer several items with different states and add an Arabic observation.
- Wait for the saved indicator, return Home, and check the visit and progress.
- Close and reopen the app, then verify the visit's answers and note are still present. A reproducible process-restart check is `adb -s SERIAL shell am force-stop dz.inspecta.pocket`, followed by the launch command above.
- Reopen the visit, edit an answer or note, and confirm the change survives another restart.
- Review the summary and share the report; verify that institution, date, subject, results, and notes are included.
- Check all screens on the approximately 360 dp-wide device for clipped Arabic text, difficult touch targets, keyboard overlap, navigation problems, and crashes.

Build, automated-test, lint, device-installation, and restart-persistence validation are **pending confirmation** in this working session. Record actual commands and outcomes in `docs/ACCEPTANCE.md`; source code alone is not evidence of acceptance.

## Known limitations

- This is a debug MVP with a demonstrative checklist, not a certified inspection standard.
- Visit results and observations can be edited; visit deletion and metadata editing are not yet available.
- Reports are shared as text. There is no PDF generation or exported file attachment.
- There are no accounts, cloud synchronization, camera, maps, analytics, or background services.
- Android backup is disabled. Uninstalling the app or clearing its storage deletes local inspections. Share any report you need to retain before doing either.

Possible later extensions include configurable checklists, visit metadata editing and deletion, local backup/import, and a signed release build. Acceptance defects take priority over new functionality.
