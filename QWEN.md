# QWEN.md

This file provides project-specific guidance for Qwen Code when working in this repository.

## Project Overview

ZeroTermux is an Android application derived from Termux. It extends the upstream Termux app with additional user-facing features such as backup/restore, container and Linux distribution management, source/mirror switching, X11 integration, FTP/HTTP/file services, OTG support, QEMU-related workflows, AI/settings screens, and other ZeroTermux-specific utilities.

The project is a multi-module Gradle Android project using Java, Kotlin, AndroidX, Material Components, and native NDK components.

### Main Modules

- `app` — Main Android application module (`namespace "com.termux"`). Contains the Termux app, ZeroTermux additions under `com.termux.zerocore`, Android resources, native bootstrap integration, assets, and application manifest.
- `terminal-emulator` — Android library module for terminal emulation logic (`namespace "com.termux.emulator"`). Contains extensive unit tests for terminal behavior.
- `terminal-view` — Android library module for terminal UI rendering and interaction (`namespace "com.termux.view"`). Depends on `terminal-emulator`.
- `termux-shared` — Android library module for shared Termux utilities (`namespace "com.termux.shared"`). Depends on `terminal-view` and includes shared Android/native helpers.

### Important Source Areas

- `app/src/main/java/com/termux/app` — Core Termux app activities, services, terminal session integration, and app lifecycle.
- `app/src/main/java/com/termux/zerocore` — ZeroTermux-specific features, settings screens, utilities, dialogs, FTP/HTTP services, AI integrations, container/QEMU/X11 helpers, and custom UI.
- `app/src/main/res` — Android layouts, strings, drawables, themes, menus, and XML configuration resources.
- `app/src/main/assets` — Bundled runtime assets used by the app.
- `app/src/main/cpp` — Native build inputs and bootstrap archive handling for the app.
- `terminal-emulator/src/main` and `terminal-emulator/src/test` — Terminal engine implementation and unit tests.
- `terminal-view/src/main` — Terminal view rendering layer.
- `termux-shared/src/main` — Shared Java/native utilities.

## Build Environment

Project configuration currently indicates:

- Gradle wrapper: use `./gradlew` from the repository root.
- Android Gradle Plugin: `com.android.tools.build:gradle:8.13.2`.
- Kotlin Gradle plugin: `2.2.0`.
- Java/JDK: 17 is used by CI and `app` Kotlin/Java configuration.
- Android SDK:
  - `compileSdkVersion=36`
  - `targetSdkVersion=28`
  - `minSdkVersion=23`
- Android NDK: `29.0.14206865` by default, overridable via `JITPACK_NDK_VERSION`.
- ABI outputs include `x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a`, plus universal APK output.

CI installs JDK 17, Android API 36, build-tools 36.0.0, and NDK 29.0.14206865 before building.

## Building and Running

Run commands from the repository root.

### Build Debug APK

```bash
./gradlew :app:assembleDebug
```

### Build Release APK

```bash
./gradlew :app:assembleRelease
```

### Build Debug and Release APKs Like CI

```bash
./gradlew --build-cache --stacktrace :app:assembleDebug :app:assembleRelease
```

### Clean Build Outputs

```bash
./gradlew clean
```

`clean` also removes downloaded `bootstrap-*.zip` files from `app/src/main/cpp` through the app module's clean hook.

### Download Nightly Termux X11 AAR Assets

The root Gradle build defines:

```bash
./gradlew downloadNightlyTermuxX11Aar
```

This downloads `termux-x11.aar` into `app/libs/` and `aisle_zt_loader.apk` into `app/src/main/assets/x11/`, verifying SHA-256 checksums from the configured GitHub release. It may use `GITHUB_TOKEN` if set.

### Bootstrap Downloads

The `app` module defines `downloadBootstraps`, which downloads Termux bootstrap zip files for all supported architectures and verifies hashes. It is wired into Java compilation and NDK build tasks via `afterEvaluate`, so normal app builds may perform network downloads if local bootstrap files are missing or have the wrong checksum.

### Install/Run on Device

A typical debug install command is:

```bash
./gradlew :app:installDebug
```

Use Android Studio or `adb`/Gradle install tasks to run on a connected Android device or emulator. Because this app uses Termux-like runtime behavior, native components, external storage access, and many Android permissions, device testing is usually more representative than plain JVM tests.

## Testing and Verification

### Run All Unit Tests

```bash
./gradlew test
```

### Run App Unit Tests

```bash
./gradlew :app:testDebugUnitTest
```

The app module uses JUnit 4 and Robolectric.

### Run Terminal Emulator Unit Tests

```bash
./gradlew :terminal-emulator:testDebugUnitTest
```

`terminal-emulator` contains the most substantial test suite, covering terminal escape sequences, cursor behavior, screen buffer behavior, keyboard handling, Unicode width, and related emulator logic.

### Run Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

Only limited instrumentation test coverage is present. This requires a connected device or emulator.

### Lint

```bash
./gradlew lint
```

The app lint configuration disables `ProtectedPermissions`. Do not assume lint is clean without running it.

### Recommended Checks After Code Changes

For most code changes, run the narrowest relevant test first, then a broader build check:

```bash
./gradlew :terminal-emulator:testDebugUnitTest
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

For release-sensitive or CI-related changes, also run:

```bash
./gradlew --build-cache --stacktrace :app:assembleDebug :app:assembleRelease
```

## Development Conventions

### Formatting

The repository has `.editorconfig` with:

- UTF-8
- LF line endings
- final newline required
- 4-space indentation for most files
- 2-space indentation for YAML files

Follow existing local style in each file. Some upstream Termux Java files use tabs or legacy formatting in places; preserve surrounding style when editing existing code.

### Language and Framework Usage

- The codebase is mostly Java with some Kotlin additions.
- Use AndroidX APIs and Material Components consistent with existing modules.
- Do not introduce new libraries unless the dependency is clearly needed and approved or already established in Gradle files.
- `app` uses Java 17/Kotlin JVM target 17, while library modules still configure Java 8 compatibility in several places. Preserve module-specific compatibility unless intentionally changing build configuration.
- Native code is built with `ndkBuild`; avoid replacing it with CMake or another build system unless explicitly requested.

### ZeroTermux Change Markers

Many files use comments such as:

```java
// ZeroTermux add {@
// @}
// ZeroTermux modify {@
// @}
// ZeroTermux delete {@
// @}
```

Preserve these markers when editing nearby code. If adding or modifying code in a region that already uses these markers, follow the established marker style.

### Android Manifest and Permissions

`app/src/main/AndroidManifest.xml` declares many sensitive Android permissions, exported components, service declarations, aliases, and feature requirements. Be cautious when editing:

- `android:exported`
- custom permissions such as `${TERMUX_PACKAGE_NAME}.permission.RUN_COMMAND`
- storage, package install/delete, overlay, boot, SMS/contact, USB, and protected permissions
- launcher activity/alias behavior
- app process/runtime assumptions inherited from Termux

Security-sensitive manifest changes should be reviewed carefully and tested on device.

### Signing Configuration

The app build uses `phone.jks` and environment-variable overrides for signing values:

- `KEY_ALIAS`
- `KEY_PASSWORD`
- `STORE_PASSWORD`

Do not expose, log, rotate, delete, or replace signing material unless explicitly requested. Avoid adding signing secrets to documentation, logs, or commits.

### Resources and UI

- Place UI text in string resources where practical instead of hardcoding user-visible strings.
- Match existing activity/theme patterns such as `Theme.AppCompat.NoActionBar` and existing `BaseTitleActivity` usage in ZeroTermux settings screens.
- Existing ZeroTermux UI code often uses XML layouts, `findViewById`, AppCompat/CardView, and direct intent navigation. Keep new code consistent unless refactoring is requested.

### Tests

- Use JUnit 4 conventions for unit tests.
- Terminal emulator tests often extend `TerminalTestCase`; prefer nearby patterns when adding emulator behavior tests.
- App tests currently have limited coverage and may use Robolectric. Verify dependencies before adding new test frameworks.
- If changing terminal parsing/emulation behavior, add or update focused tests under `terminal-emulator/src/test/java/com/termux/terminal`.

## CI and Releases

GitHub Actions workflow `.github/workflows/android.yml` builds on pushes, pull requests, and manual dispatch. It:

- runs on Ubuntu latest
- uses JDK 17
- installs Android SDK platform 36, build-tools 36.0.0, and NDK 29.0.14206865
- runs `./gradlew --build-cache --stacktrace :app:assembleDebug :app:assembleRelease`
- packages debug and release APKs by ABI and universal output
- uploads artifacts
- creates GitHub releases for tags

The workflow triggers for `main`/`master` and tags matching `ZeroTermux-*`, `v*`, or numeric tags. Commits beginning with `[skip ci]` skip push builds.

Be careful when editing CI, release naming, APK output names, or signing-related configuration because those changes affect published artifacts.

## Dependency Notes

The app module includes many third-party dependencies, including but not limited to AndroidX, Material Components, Guava, Gson, OkGo, XXPermissions, Aria, libsu, RootBeer, libaums, AgentWeb, ColorSeekBar, ImagePicker, SmartSwipe, Glide, ImmersionBar, NanoHTTPD, jsoup, Room, biometric APIs, Rosemoe editor, and local jars/AARs under `app/libs`.

Before using a dependency in new code:

1. Confirm it is already declared in the relevant module.
2. Prefer existing project utilities and wrappers where present.
3. Avoid adding new transitive dependency risk unless necessary.

## Common Pitfalls

- Builds may download bootstrap files or X11 assets from GitHub; network availability can affect local builds.
- `app` has duplicate/overlapping dependencies and legacy compatibility settings; avoid broad dependency upgrades unless specifically requested.
- Manifest permissions and exported components are security-sensitive.
- The project mixes upstream Termux code with ZeroTermux modifications; do not remove upstream behavior unless explicitly asked.
- Some tests may be sparse or commented out in app-level code; do not assume a passing test suite fully validates UI/runtime behavior.
- This repository includes native code and ABI-specific APK outputs; test changes across affected ABIs when native or packaging code changes.

## Working Guidelines for Future Agents

- Start by reading the relevant module's `build.gradle`, nearby source files, and existing tests before modifying code.
- Use absolute paths with tools.
- Prefer small, localized edits that preserve existing style and ZeroTermux markers.
- Add or update tests for behavior changes where feasible.
- Run relevant Gradle tests/builds after modifications and report any commands that could not be run.
- Do not push, release, delete signing assets, or change CI/release behavior without explicit user instruction.
