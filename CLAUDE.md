# CLAUDE.md — Notes App

## Project
Native Android notes app (Apple Notes-style, not Google Keep): hierarchical nested folders, rich-text notes, favorites, trash with 30-day retention, note/folder locking via biometric or device credential, local-only storage with Android Auto Backup, light/dark/dynamic themes. In-app UI strings and the Play Store listing are both localized into 8 languages — EN (source of truth), IT, DE, FR, ES-ES, ES-419, PT-PT, PT-BR — kept in lockstep: `res/values/`, `values-it/`, `values-de/`, `values-fr/`, `values-es/`, `values-b+es+419/`, `values-pt-rPT/`, `values-pt-rBR/` for in-app strings, and `play/listing/<locale>/` for the Play Store listing (title/short/full description). Settings exposes an in-app language switcher (`AppLanguage`, independent of the device locale) covering the same 8 languages, so every new user-facing string must ship in all 8 `values*` folders, not just EN+IT. Published on Play Store. No backend in v1, but the data model is sync-ready. Future (do NOT implement, do NOT preclude): images, tables, full-text search, sharing, multi-device sync.

Editor v1 scope: headings (H1/H2/H3), bold, italic, underline, text color, bullet lists, numbered lists, clickable links.

## Tech stack
- Kotlin 2.4.x · AGP 9.x (built-in Kotlin) · Gradle Kotlin DSL · version catalog at `gradle/libs.versions.toml` · KSP2 · JDK 17 · minSdk 26 · targetSdk 37
- Jetpack Compose (stable BOM) with `material3` overridden to the latest **1.5.0-alpha** for Material 3 Expressive (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` where required)
- Single Activity, no Fragments, no XML layouts
- Navigation Compose 2.9.x with type-safe `@Serializable` routes
- MVVM + unidirectional data flow · layers: `ui` / `domain` / `data`, with `designsystem/`, `di/`, and `security/` as supporting packages · app module `:app`, plus a `:baselineprofile` module (`androidx.baselineprofile`, targets `:app`) that only generates the release baseline profile — it is not a feature module and carries no app logic
- Room (KSP) as single source of truth · kotlinx.serialization (note content as JSON block document in a Room column, mirrored through a data-layer-only `NoteDocumentDto`) · DataStore Preferences (settings) · kotlinx-datetime
- richeditor-compose + ksoup (HTML) bridge the WYSIWYG editor's HTML state to/from the domain `NoteDocument` block model — the toolbar and serialized model must be kept in sync when either changes
- Hilt (KSP) for DI
- androidx.biometric for app lock (`BIOMETRIC_WEAK or DEVICE_CREDENTIAL`)
- No networking libraries in v1

## Commands
- Build: `./gradlew assembleDebug`
- **Full verification (ALWAYS run before declaring any work done):** `./gradlew detekt lint testDebugUnitTest koverVerify`
- JVM tests only: `./gradlew testDebugUnitTest`
- Single test class: `./gradlew testDebugUnitTest --tests "com.anacardix.jottiq.SomeTest"`
- Instrumented smoke suite (emulator required; CI runs it on PRs): `./gradlew connectedDebugAndroidTest` — this excludes the `com.anacardix.jottiq.playscreenshots` package (Play Store screenshot capture tests) via `testInstrumentationRunnerArguments["notPackage"]`; those run only through the dedicated Gradle Managed Device invocation described in `play/README.md`, never in CI
- Install on device: `./gradlew installDebug`

## Architecture rules
- UI: stateless composables. Each screen has one ViewModel exposing a single `StateFlow<XxxUiState>` and an `onEvent(XxxEvent)` entry point. No business logic in composables.
- Domain: pure Kotlin, zero Android imports. Use cases only when they contain real logic — no pass-through use cases.
- Data: repositories are the only source of truth. Room entities and DAOs never leave the data layer; map to domain models at the repository boundary.
- Errors: the data layer catches and maps failures into a typed `DataResult<T>` (`Success`/`Failure` over a sealed `DataError`) via `runCatchingDataResult`; ViewModels translate `Failure` into UiState (user-visible messages via string resource ids). Never swallow exceptions silently. Always rethrow `CancellationException`.
- **Sync-ready invariants (never break):** entity ids are client-generated UUID strings; every entity has `createdAt`/`updatedAt` as epoch millis UTC; deletion is soft (`deletedAt` nullable — this also powers the trash); hard-delete happens only in trash purge (30 days or manual).
- Note content = `NoteDocument`: an ordered list of blocks (`Paragraph`, carrying an optional heading level plus bulleted/numbered flags; future: `Image`, `Table`, …), each text block carrying formatting spans (bold, italic, underline, color, link) as ranges. Serialized to JSON via kotlinx.serialization into a single Room column. Extend by adding new block types; never rename existing serialized fields.
- Locking: the gate lives behind the `AppLockManager` interface in `security/`. `BiometricPrompt` code exists only inside its implementation. A folder's lock protects its entire subtree.
- Keep Material 3 Expressive API usage wrapped inside `designsystem/` where feasible, so the future upgrade to stable 1.5.0 touches one package.

## Testing (non-negotiable)
- **Every new feature or bugfix MUST ship with corresponding tests.**
- **Before declaring ANY task complete: run `./gradlew detekt lint testDebugUnitTest koverVerify`, read the real output, and confirm it passed. Never assume or claim success without running it.**
- Test placement: JVM tests live in `src/test` — plain unit tests AND Robolectric tests (Room DAOs, migrations, Compose UI). `src/androidTest` holds a minimal end-to-end smoke suite plus the separate `playscreenshots` package (Play Store screenshot capture, excluded from CI — see Commands).
- Frameworks: JUnit 4 everywhere · kotlinx-coroutines-test (`runTest`) · Turbine for Flow assertions · Truth for assertions.
- Mocking policy: fakes-first. In-memory fakes for repositories/DAOs live under `src/test/.../fakes` and are reused across tests. MockK only for platform APIs that cannot be faked.
- Mandatory coverage: domain and data packages ≥ 80% line coverage, enforced by `koverVerify`.
- Every Room schema change requires a migration and a migration test.

## Conventions
- detekt (with detekt-formatting) is the style authority; `./gradlew detekt` must pass with zero issues. Config: `config/detekt/detekt.yml`.
- Naming: `*Screen`, `*ViewModel`, `*UiState`, `*Event`, `*Repository` (interface) / `*RepositoryImpl`, `*Dao`, `*Entity`, `Fake*`. Composables in PascalCase.
- Test names: backtick sentences — `` fun `moving note to trash sets deletedAt`() ``.
- All user-facing strings go in `res/values/strings.xml` and all 7 translated `values-*/strings.xml` folders (see Project section for the full list). Never hardcode UI text.
- Commits: Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`, `docs:`).
- Branches: `feature/<short-name>`, `fix/<short-name>`; PRs target `main`; `main` must always be green.

## Always
- Run the full verification command and read its actual output before saying a task is done.
- Add every new user-facing string to `values/` and all 7 translated `values-*/` folders.
- Add every new dependency to `gradle/libs.versions.toml` (never inline versions in build files).
- Write a Room migration + migration test for every schema change.

## Never
- Never claim tests pass without having run them in this session.
- Never expose DAOs or Room entities outside the data layer.
- Never hard-delete user data outside trash purge.
- Never break the sync-ready invariants (UUID ids, timestamps, soft delete).
- Never suppress a detekt/lint finding without a comment explaining why.
- Never introduce Fragments, XML layouts, or a second Activity.
