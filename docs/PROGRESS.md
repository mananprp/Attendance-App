# Project Progress & Status Tracker

**Project:** Face-Verified Attendance App (Android + Backend)  
**Current Date:** 2026-09-30  
**Phase:** Wave 0 - Contracts, Architecture & Repo Scaffolding  

---

## Wave Status Summary

| Wave | Description | Status | Gate Check |
|---|---|---|---|
| **Wave 0** | Contracts, Schemas, Scaffolds & Shared Interfaces | **COMPLETED** | Contracts written, projects scaffolded, interfaces defined |
| **Wave 1** | Core (Agent 1), Design (Agent 2), Face (Agent 5), Backend (Agent 8) | **COMPLETED** | Core, Design system, ONNX Face pipeline & Backend built + tested |
| **Wave 2** | Onboarding (Agent 3), Punch (Agent 4), Sync (Agent 6), Calendar (Agent 7) | **COMPLETED** | Auth/Onboarding, Punch state machine, Sync engine & Calendar built + tested |
| **Wave 3** | QA, Integration & End-to-End Testing (Agent 9 + Agent 0) | **COMPLETED** | Manual device test matrix written, cross-cutting integration verified, 100% tests pass |
| **Wave 4** | Mobile UI Revamp (Findez Aesthetic Dark Theme) | **COMPLETED** | Full Findez design overhaul, tokens, 14 vector drawables, Work Ring, Swipe to Punch, Calendar, Bottom Bar |

---

## Agent Progress Logs

### Agent 0: Orchestrator and Architect
- **Status:** Wave 0 Completed, contracts ready for review.
- **Deliverables:**
  - `docs/openapi.yaml`: Complete REST OpenAPI 3.0.3 contract.
  - `docs/schema.sql`: Production MySQL 8.0 DDL with relational integrity, unique photo limits, constraints.
  - `docs/room-schema.md`: Complete SQLite/Room entity and DAO specifications.
  - `server/`: Scaffolded NestJS TypeScript project with TypeORM, JWT auth, validation pipes, Swagger/OpenAPI setup, seeds, and e2e test structure.
  - `android/`: Scaffolded Android project with Kotlin, Jetpack Compose, Material 3, Hilt DI, Room, WorkManager, CameraX, ONNX Runtime Android dependencies, and shared interfaces.
  - Shared interfaces defined in `com.company.attendance.core.interfaces`:
    - `TrustedTimeProvider`
    - `FaceVerifier`
    - `LocationChecker`
    - `PunchRepository`
    - `SyncScheduler`
  - `docs/CONTRACT_CHANGES.md` & `docs/PROGRESS.md` created.
- **Known Gaps / Blocked:** None. Ready for human review of contracts.

### Agent 1: Android Core
- **Status:** Wave 1 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/core/`, `android/app/src/test/java/com/company/attendance/core/`
- **Scope:** Hilt DI, Room database & DAOs, Retrofit API client + AuthInterceptor, Secure Token Storage (EncryptedSharedPreferences / Keystore), RemoteConfig fetch & cache, `TrustedTimeProvider` SNTP + monotonic clock + offline fallback, `LocationChecker` Haversine geofence, `PunchRepository` persistence & missed punch rollover.
- **Deliverables Completed:**
  - **Room Database & DAOs:** `UserLocalEntity`, `PunchEntity`, `DaySummaryEntity`, `AppConfigEntity`, `UserLocalDao`, `PunchDao`, `DaySummaryDao`, `AppConfigDao`, `AppDatabase` (version 1), `EntityMappers`.
  - **Network & API:** `AttendanceApiService` matching `openapi.yaml`, full Network DTOs, `AuthInterceptor` (attaches Bearer token), `ApiException`, `NetworkHelper`.
  - **Storage & Config:** `TokenStorage` / `TokenStorageImpl` (EncryptedSharedPreferences with fallback), `RemoteConfigRepository` / `RemoteConfigRepositoryImpl` (fetch + Room cache).
  - **Trusted Time Provider:** `TrustedTimeProviderImpl` with SNTP multi-pool client (`pool.ntp.org`, `time.google.com`, `time.cloudflare.com`), monotonic `SystemClock.elapsedRealtime()` anchoring against clock tampering, office timezone conversion (`Asia/Kolkata`), date boundary handling, offline fallback (`isUnverified = true`).
  - **Location Checker:** `LocationCheckerImpl` with Haversine WGS84 formula, permission validation, location services check, `GeofenceResult` calculation.
  - **Punch Repository:** `PunchRepositoryImpl` with atomic punch recording, `day_summary` state machine transitions, and business rule FR-17 (next-day first punch marks earlier unclosed days `MISSED_PUNCH` with `out_time = in_time`).
  - **Hilt DI Modules:** `DatabaseModule`, `NetworkModule`, `CoreModule`.
  - **Unit Tests:** `TrustedTimeProviderTest`, `LocationCheckerTest`, `PunchRepositoryTest`, `AuthInterceptorTest`, `EntityMappersTest`, `RemoteConfigRepositoryTest`.

### Agent 2: Design System and Navigation
- **Status:** Wave 1 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/design/`, `android/app/src/test/java/com/company/attendance/design/`
- **Scope:** Light-only theme (colors, typography, shapes), custom buttons, status chips, day summary cards, calendar cell, bottom navigation bar, top bars, state views, text fields, day detail bottom sheet, component gallery preview, and Navigation graph.
- **Deliverables Completed:**
  - **Theme Tokens & System:**
    - `Color.kt`: Exact light palette (#FAFAFA background, #FFFFFF surface, #111827 primary text, #6B7280 secondary text, #E5E7EB border, #2563EB accent, #EFF4FF accent tint, #16A34A success, #D97706 warning, #DC2626 error, disabled/tint states), Material 3 `LightColorScheme`.
    - `Type.kt`: System Roboto typography scale (ClockTime 40sp medium, Title 22sp semibold, SectionHeading 16sp medium, Body 14sp regular, Caption 12sp regular, ButtonText 16sp semibold), Material 3 `AttendanceTypography`.
    - `Dimensions.kt`: 8dp base grid, 20dp screen padding, 12dp card/button corners, 52dp button height, 1dp borders/dividers, 48dp min touch target, 42dp calendar cell size.
    - `Shape.kt`: 8dp small (chips), 12dp medium/large (cards, buttons) `AttendanceShapes`.
    - `Theme.kt`: `AttendanceTheme` enforcing light-theme only, light status & navigation bar icons.
  - **Design System UI Components:**
    - `Buttons.kt`: `PrimaryButton` (accent filled, disabled gray, loading spinner), `SecondaryButton` (outlined 1dp border), `DangerButton` (error filled), `AttendanceTextButton` (ghost action button).
    - `StatusChip.kt`: Rounded 8dp chip with colored dot and tint (`DayStatusChip` for COMPLETE/MISSED_PUNCH/NO_RECORD, `SyncStatusChip` for SYNCED/PENDING/FAILED).
    - `TimeDisplayCard.kt`: 12dp card with 1dp border displaying Punch In / Out times (dashes `--:--` when empty).
    - `TopAppBar.kt`: `AttendanceTopAppBar` minimalist title-only top bar with back navigation and bottom 1dp border.
    - `BottomNavBar.kt`: `AttendanceBottomNavBar` with Home & Calendar line icons, accent selection indicator, and top divider.
    - `CalendarDayCell.kt`: 42dp calendar cell with accent selection fill, today 1dp outline, status dots (green complete, amber missed punch), and dimmed non-current month styling.
    - `StateViews.kt`: `LoadingState` (spinner with text), `EmptyState` (line icon + action), `ErrorStateWithRetry` (error icon + message + retry button).
    - `TimeUnverifiedBadge.kt`: Warning badge for offline punches using device clock.
    - `TextFields.kt`: `AttendanceTextField` with 12dp corners, 1dp border, accent focus indicator, error messaging, password toggle support.
    - `DayDetailSheet.kt`: `DayDetailSheetContent` bottom sheet layout for calendar day inspection.
    - `SyncStatusBar.kt`: Status row with pending upload count, last sync timestamp, and "Sync Now" action.
  - **Navigation & App Scaffolding:**
    - `Screen.kt`: Sealed navigation destination routes (`Login`, `OnboardingWizard`, `ModelDownload`, `Home`, `PunchCapture`, `Calendar`, `ProfileSettings`, `ComponentGallery`).
    - `AttendanceNavGraph.kt`: Top-level NavHost with composable destination hooks and placeholder screens.
    - `AppScaffold.kt`: Top-level scaffold managing navigation state, bottom bar visibility for top-level screens, and snackbars.
    - `MainActivity.kt`: Integrated with `AttendanceAppScaffold()`.
  - **Gallery & Testing:**
    - `ComponentGalleryScreen.kt`: Interactive design gallery showcasing all tokens, typography, buttons, chips, cards, textfields, calendar cells, state views, and bottom bar, with Compose `@Preview`.
    - `ThemeTokensTest.kt` & `NavigationRoutesTest.kt`: Unit tests verifying color hex values, typography scale, dimension tokens, status enums, route paths, and bottom navigation visibility rules.

### Agent 3: Auth & Onboarding
- **Status:** Wave 2 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/feature/auth/`, `feature/onboarding/`, `android/app/src/test/java/com/company/attendance/feature/`
- **Scope:** Login screen, onboarding wizard (camera perm -> live photo capture with single face check -> upload v1 -> location perm -> done), one-time photo change flow, history pull on login, logout.
- **Deliverables Completed:**
  - **Login Flow (`feature/auth/`):**
    - `LoginUiState.kt` & `LoginNavigationEvent`: Unidirectional state model for login form, input validation, and navigation branching (`NavigateToHome` vs `NavigateToOnboarding`).
    - `LoginViewModel.kt`: Input validation, authentication against `POST /v1/auth/login`, session token persistence in `TokenStorage` (EncryptedSharedPreferences), profile caching in Room `user_local`, remote config fetch & cache via `RemoteConfigRepository`.
    - **Initial History Sync:** When user with existing photo logs in (`hasPhoto == true`), automatically triggers initial history pull from `GET /v1/attendance?month=YYYY-MM` into SQLite via `PunchRepository.upsertDaySummaries`.
    - `LoginScreen.kt`: Material 3 login screen with employee ID, password visibility toggle, inline error messages, loading spinner, and automated navigation routing.
  - **Onboarding Wizard (`feature/onboarding/`):**
    - `OnboardingModels.kt`: Wizard steps (`CAMERA_PERMISSION`, `PHOTO_CAPTURE`, `PHOTO_CONFIRMATION`, `LOCATION_PERMISSION`, `COMPLETED`), state model with bitmap, embedding, and progress tracking.
    - `FaceGuideOverlay.kt`: Oval cutout overlay with PorterDuff Clear blending, guidance border, and instructions.
    - `CameraPermissionStep.kt`: Permission request UI explaining on-device face biometric requirement.
    - `PhotoCaptureStep.kt`: CameraX front camera preview, oval guide framing, capture action, on-device face validation with `FaceVerifier.extractReferenceEmbedding(bitmap)` ensuring single-face constraint, and verification badge.
    - `LocationPermissionStep.kt`: Location permission request explaining 100m geofence requirement.
    - `OnboardingCompleteStep.kt`: Completion checklist screen with navigation to Home.
    - `OnboardingViewModel.kt`: Step transition management, photo v1 upload to `POST /v1/users/me/photo`, embedding and version persistence to Room `user_local` and `TokenStorage`.
    - `OnboardingScreen.kt`: Animated crossfade wizard coordinator with top step counter and linear progress indicator.
  - **Profile & Photo Change (`feature/auth/profile/`):**
    - `ProfileUiState.kt` & `ProfileNavigationEvent`: State model capturing employee ID, photo version, and `canChangePhoto` state.
    - `ProfileViewModel.kt`: Displays employee details and photo version, strictly enforces photo-once rule (`photoChangeUsed == false`), manages photo replacement flow via `PUT /v1/users/me/photo` with `FaceVerifier` reference embedding extraction, handles server 403 `PHOTO_CHANGE_EXHAUSTED` responses, and provides sign-out functionality.
    - `ProfileScreen.kt`: Clean profile screen showing employee avatar, photo version badge, one-time photo change action, component gallery link, sign-out button, and full-screen CameraX photo replacement dialog.
  - **Shared Utilities (`feature/auth/util/`):**
    - `EmbeddingUtils.kt`: Exact FloatArray (512-d) <-> ByteArray conversion using little-endian byte buffers.
    - `ImageUtils.kt`: Bitmap to JPEG multipart request conversion.
  - **Navigation Integration:**
    - `AttendanceNavGraph.kt`: Wired real `LoginScreen`, `OnboardingScreen`, and `ProfileScreen` destinations.
  - **Unit Tests (`android/app/src/test/java/com/company/attendance/feature/`):**
    - `LoginViewModelTest.kt`: Validations, session persistence, remote config fetch, history sync into Room, and onboarding branching.
    - `OnboardingViewModelTest.kt`: Wizard progression, single face detection pass/fail guidance, photo v1 upload, and permission state updates.
    - `ProfileViewModelTest.kt`: Profile detail loading, photo-once rule enforcement, photo v2 replacement, 403 limit handling, and sign-out.

### Agent 4: Punch Flow
- **Status:** Wave 2 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/feature/punch/`, `android/app/src/test/java/com/company/attendance/feature/punch/`
- **Scope:** Home screen punch state machine, 100m geofence verification, CameraX 2.5s frame capture & best frame selection, FaceVerifier invocation, SQLite persistence, midnight reset, next-day missed punch resolution.
- **Deliverables Completed:**
  - **Home Screen & State Machine (`feature/punch/`):**
    - `HomeUiState.kt`: Unidirectional state representation for greeting, live office local date & trusted time, unverified offline clock badge, today's punch in / punch out timestamps, button states (`isPunchInEnabled`, `isPunchInBlocked`, `isPunchOutEnabled`, `isPunchOutBlocked`), sync status bar metrics, and geofence dialog states (`OutsideRadius`, `PermissionRequired`, `LocationServicesDisabled`, `LocationUnavailable`).
    - `HomeViewModel.kt`: Core punch state machine implementing business rules:
      - Initial (no punch today): Punch In enabled, Punch Out disabled.
      - After Punch In: Punch In blocked (shows recorded time), Punch Out enabled.
      - After Punch Out: Both buttons blocked (shows recorded times).
      - Midnight rollover: Listens to `TrustedTimeProvider.currentTimeFlow`, detects local date boundary changes at 00:00:00, and automatically triggers state refresh and button reset for the next day.
      - Geofence verification: Validates location proximity against Remote Config (`officeLat`, `officeLng`, `radiusMeters`) via `LocationChecker.checkGeofence` before navigating to capture screen. Displays refusal alert with calculated distance if outside radius.
      - Missed punch resolution: Invokes `PunchRepository.resolvePastMissedPunches` on punch initiation to resolve prior incomplete days.
      - Sync trigger: Interacts with `SyncScheduler.triggerSyncNow()` and observes live pending upload count and last sync timestamp.
    - `HomeScreen.kt`: Jetpack Compose screen implementing minimal light UI:
      - Header greeting with dynamic time-of-day greeting ("Good morning", "Good afternoon", "Good evening") and employee display name.
      - Large 40sp trusted clock (`AppTypographyTokens.ClockTime`) with `TimeUnverifiedBadge` for offline clock fallback.
      - `TimeDisplayCard` rendering today's Punch In / Punch Out times with dashes (`--:--`) when empty.
      - `PrimaryButton` / `SecondaryButton` for state-managed Punch In / Punch Out actions.
      - Bottom-aligned `SyncStatusBar` with pending badge and immediate "Sync Now" trigger.
      - `GeofenceErrorAlertDialog` for geofence and permission refusals.
  - **Punch Capture Flow (`feature/punch/capture/`):**
    - `PunchCaptureUiState.kt`: Capture steps (`INITIALIZING`, `CAPTURING`, `PROCESSING`, `SUCCESS`, `FAILURE`), progress indicator (0f-1f), score metrics, and failure diagnostics.
    - `CameraCaptureUtils.kt`: App-private image persistence (`files/punches/<uuid>.jpg`), CameraX `ImageProxy` to rotated `Bitmap` conversion, and 512-d float array to byte array BLOB serialization.
    - `PunchCaptureViewModel.kt`: Full capture orchestration:
      - Validates ONNX model availability and user reference embedding.
      - Samples camera preview frames at ~10 FPS over a 2.5-second capture window.
      - Dispatches captured frames to `FaceVerifier.verifyPunchFrames(frames, referenceEmbedding, threshold)`.
      - On `Match`: Saves aligned face image to internal storage, acquires authoritative trusted time, records punch via `PunchRepository.recordPunch()`, and transitions to `SUCCESS`.
      - On `Mismatch`, `NoFaceDetected`, `MultipleFacesDetected`, `LivenessFailed`, `ModelMissingOrCorrupt`, `Error`: Sets appropriate failure explanation and allows "Try Again" without consuming punch attempt.
    - `PunchCaptureScreen.kt`:
      - Fullscreen CameraX front-camera preview using `AndroidView` + `PreviewView` + `ImageAnalysis`.
      - Custom Canvas `FaceOvalGuideOverlay` with darkened backdrop and oval cutout.
      - Top guidance banner with real-time `LinearProgressIndicator`.
      - Processing overlay during neural pipeline inference.
      - Success screen with recorded time, date, match confidence chip, and return action.
      - Failure screen with detailed reason and "Try Again" / "Cancel" buttons.
  - **Unit Test Suites (`android/app/src/test/java/com/company/attendance/feature/punch/`):**
    - `HomeViewModelTest.kt`: 10 comprehensive unit test cases verifying:
      - Initial state button readiness (In enabled, Out disabled).
      - Post Punch In transition (In blocked with time, Out enabled).
      - Post Punch Out transition (Both blocked with times).
      - Midnight rollover and button reset for the next day at 00:00.
      - Time-of-day greeting calculations.
      - Geofence inside radius navigation and missed punch resolution.
      - Geofence outside radius refusal dialog with distance formatting.
      - Location permission and location services disabled error handling.
      - Sync Now trigger execution.
    - `PunchCaptureViewModelTest.kt`: 9 comprehensive unit test cases verifying:
      - Model readiness validation.
      - Missing reference embedding failure.
      - Successful face match, trusted time acquisition, and punch recording.
      - Face mismatch failure and no punch recording.
      - No face detected handling.
      - Multiple faces detected violation.
      - 3D liveness check failure.
      - "Try Again" capture reset without attempt consumption.
      - Navigation event dispatching.

### Agent 5: Face Pipeline (ONNX)
- **Status:** Wave 1 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/face/`, `tools/`
- **Scope:** Sequential ONNX model loader/releaser (`det_10g`, `1k3d68`, `w600k_r50`), model downloader with checksum verification, landmark liveness detection, embedding cosine similarity, parity test scripts.
- **Deliverables Completed:**
  - **Data Models (`face/model/`):**
    - `FaceDetection.kt`: Bounding box (`RectF`), 5 landmarks (`PointF`), score, crop area, quality metrics.
    - `FaceLandmarks3D.kt`: 68 3D landmark coordinates, yaw proxy estimation.
    - `LivenessModel.kt`: `LivenessSignals`, `LivenessVerdict`, `FrameRecord`.
    - `ModelDownloadState.kt`: `ModelType` (`det_10g`, `1k3d68`, `w600k_r50`), `DownloadState`, `ModelDownloadProgress`.
  - **Math & Image Utilities (`face/util/`):**
    - `MathUtils.kt`: L2 vector normalization, cosine similarity, Umeyama 2D similarity transform estimation for ArcFace 112x112, 2D affine inversion and point mapping, Procrustes / Affine least-squares nonrigid deformation residual, Eye Aspect Ratio (EAR), Yaw proxy, percentile calculations.
    - `ImageProcessor.kt`: Aspect-ratio preserving resize with letterbox padding (640x640), NCHW direct FloatBuffer tensor conversion with normalization (`(x-127.5)/128.0` and `(x-127.5)/127.5`), affine crop warping, Laplacian variance face sharpness calculation.
  - **ONNX Sequential Pipeline (`face/onnx/`):**
    - `OnnxSessionManager.kt`: Low-memory sequential session executor (`withSession(modelType)`) ensuring only a single model session is loaded in memory at any time.
    - `ScrfdDetector.kt`: SCRFD multi-stride (8, 16, 32) anchor decoding, bounding box + 5 landmark extraction, score thresholding (>= 0.50), IoU Non-Maximum Suppression (0.40), single-face validation.
    - `Landmark3DRunner.kt`: 192x192 face crop warping, 68 3D landmark extraction, coordinate space denormalization, inverse affine coordinate mapping.
    - `ArcFaceEmbedder.kt`: 112x112 ArcFace landmark alignment, 512-d feature extraction, L2 normalization.
    - `LivenessEngine.kt`: Temporal EAR blink detection (0.75 * median), frontal reference selection, inter-ocular distance (IOD) scaling, least-squares nonrigid residual, head motion calculation, weighted liveness verdict formula `(0.5 * nonrigid + 0.3 * blinks + 0.2 * motion)`.
  - **Model Downloader (`face/download/`):**
    - `ModelDownloader.kt`: Chunked resumable HTTP downloads via OkHttp `Range: bytes=`, progress flow with byte count & percentage, SHA-256 integrity verification against manifest, atomic file replacement in app-private storage.
  - **Face Verifier Implementation (`face/verifier/`):**
    - `FaceVerifierImpl.kt`: Implements `FaceVerifier` interface:
      - `areModelsReady()`: Validates model existence in storage.
      - `extractReferenceEmbedding(image)`: Validates single face, crops and extracts 512-d reference embedding for onboarding.
      - `verifyPunchFrames(frames, referenceEmbedding, threshold)`: Runs sequential 3-pass pipeline (Detection -> 3D Liveness -> ArcFace Recognition), returns typed `FaceVerificationResult` (`Match`, `Mismatch`, `NoFaceDetected`, `MultipleFacesDetected`, `LivenessFailed`, `ModelMissingOrCorrupt`).
      - `releaseResources()`: Frees native ONNX runtime session handles.
  - **Dependency Injection (`face/di/`):**
    - `FaceModule.kt`: Hilt module providing singleton `FaceVerifier` and `ModelDownloader`.
  - **Parity Testing & Test Fixtures (`tools/`):**
    - `tools/generate_sample_embeddings.py`: Generates mathematical test vectors, sample 512-d embeddings, and reference values.
    - `tools/parity_test.py`: Validates cosine similarity, Umeyama similarity transform matrix (< 1e-6 diff), EAR / Yaw proxy formulas, least-squares nonrigid residual, and liveness scoring against Python reference.
  - **Unit Tests (`android/app/src/test/java/com/company/attendance/face/`):**
    - `MathUtilsTest.kt`: Cosine similarity, L2 norm, Umeyama alignment matrix, 2D affine inversion, EAR, Yaw proxy, percentile.
    - `LivenessEngineTest.kt`: Pass/fail/inconclusive liveness state transitions, static photo rejection, identity inconsistency rejection.
    - `ModelDownloaderTest.kt`: SHA-256 calculation, model readiness checks, download progress flow, error states.
    - `FaceVerifierStateTest.kt`: `FaceVerifierImpl` result types, missing models handling, empty frame handling, data class equality.

### Agent 6: Sync Engine
- **Status:** Wave 2 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/sync/`, `android/app/src/test/java/com/company/attendance/sync/`
- **Scope:** WorkManager background periodic sync with server-configured windows and random jitter, immediate Sync Now trigger, multipart batch upload with images, idempotent deduplication handling, exponential backoff, and local image cleanup.
- **Deliverables Completed:**
  - **WorkManager Sync Worker (`sync/work/`):**
    - `AttendanceSyncWorker.kt`: `@HiltWorker` assisted WorkManager `CoroutineWorker` querying `PunchRepository.getPendingPunches`, reading captured verification images from app-private storage, preparing multipart `batch_json` + image parts, posting to `AttendanceApiService.syncPunches`, parsing individual record statuses (`ACCEPTED`, `DUPLICATE`, `REJECTED`), marking acknowledged punches `SYNCED` in SQLite, deleting acknowledged image files from storage, recording attempt counts on failures, and executing retries with exponential backoff on network/server errors.
  - **Sync Scheduler (`sync/scheduler/`):**
    - `SyncSchedulerImpl.kt`: Implements `SyncScheduler` interface.
      - `scheduleConfiguredWindows(syncWindows, jitterMinutes)`: Calculates next trigger epoch in office timezone, adds bounded per-device random jitter (0..jitterMinutes), and enqueues periodic unique work (`ExistingPeriodicWorkPolicy.UPDATE`) with `NetworkType.CONNECTED` constraint.
      - `triggerSyncNow()`: Direct immediate upload execution updating `_syncState` StateFlow (`isSyncing`, `pendingCount`, `lastSyncedEpochMs`, `lastError`), returning typed `ManualSyncResult` (`Success`, `NoData`, `Failure`).
      - `refreshPendingCount()`: Updates reactive pending punch count.
  - **Utilities & DI (`sync/util/`, `sync/di/`):**
    - `SyncOffsetCalculator.kt`: High-precision timezone-aware time window delay calculations, next-day boundary handling, and bounded jitter distribution.
    - `SyncModule.kt`: Hilt module providing `SyncScheduler`, `WorkManager`, and background `CoroutineDispatcher`.
    - `AttendanceApp.kt` & `AndroidManifest.xml`: Configured `HiltWorkerFactory` with `Configuration.Provider` and disabled default `WorkManagerInitializer`.
  - **Unit Tests (`android/app/src/test/java/com/company/attendance/sync/`):**
    - `SyncOffsetCalculatorTest.kt`: Same-day future window calculation, past-window next-day rollover, midnight window, bounded jitter distribution, and epoch conversions.
    - `SyncSchedulerTest.kt`: WorkManager periodic unique work enqueuing, `triggerSyncNow` flows (not logged in, no data, successful upload & deduplication with image cleanup, 500 server error, network exception), and `refreshPendingCount`.
    - `AttendanceSyncWorkerTest.kt`: Worker empty/unauthenticated paths, multipart batch upload, `ACCEPTED`/`DUPLICATE` acknowledgment with image file deletion, `REJECTED` attempt recording, 503 retry, 400 failure, and network `IOException` retry.

### Agent 7: Calendar
- **Status:** Wave 2 Completed
- **Folder:** `android/app/src/main/java/com/company/attendance/feature/calendar/`, `android/app/src/test/java/com/company/attendance/feature/calendar/`
- **Scope:** Monthly attendance calendar, SQLite `day_summary` observation, 7-column date grid, status dot indicators, month navigation, and Day Detail bottom sheet with office timezone timestamps, unverified badges, and punch sync statuses.
- **Deliverables Completed:**
  - **Data Models (`feature/calendar/model/`):**
    - `CalendarUiState.kt`: `CalendarUiState`, `CalendarDayUiModel`, `DayDetailUiState`, and `MonthAttendanceSummary`.
  - **Date & Calendar Utilities (`feature/calendar/util/`):**
    - `CalendarDateUtils.kt`:
      - 7-column calendar grid generator (Sunday-first, leading/trailing non-month days flagged).
      - Month/Year formatting (`MMMM yyyy` e.g. "September 2026").
      - Day detail date formatting (`EEEE, d MMMM yyyy` e.g. "Wednesday, 30 September 2026").
      - Office timezone 12-hour punch time formatting (`hh:mm a` e.g. "09:15 AM", "06:30 PM").
      - Punch sync status derivation (`SYNCED`, `PENDING`, `FAILED`).
  - **Day Detail Bottom Sheet (`feature/calendar/detail/`):**
    - `DayDetailBottomSheet.kt`: Modal bottom sheet wrapping `DayDetailSheetContent` displaying date title, punch in/out timestamps, `DayStatusChip` (`COMPLETE`, `MISSED_PUNCH`, `NO_RECORD`), `TimeUnverifiedBadge`, and `SyncStatusChip`.
  - **Calendar ViewModel & Screen (`feature/calendar/`):**
    - `CalendarViewModel.kt`: Observes user session, trusted time, remote config timezone, and SQLite `day_summary` via `PunchRepository.observeMonthSummaries(userId, yearMonth)`. Handles previous/next month navigation, today jump, day selection with punch retrieval, and detail sheet dismissal.
    - `CalendarScreen.kt`: Minimalist light UI matching Section 8: Month navigation header (`<` / `>`), monthly stats summary strip (Complete / Missed counters), days of week row (`Sun` - `Sat`), 7-column date grid using `CalendarDayCell` (selected accent fill, today outline, status dots), and status legend.
  - **Navigation Integration:**
    - `AttendanceNavGraph.kt`: Integrated `CalendarScreen()` as default composable for `Screen.Calendar.route`.
  - **Unit Tests (`android/app/src/test/java/com/company/attendance/feature/calendar/`):**
    - `CalendarDateUtilsTest.kt`: Grid generation (35/42 cells, Sunday starts, leading/trailing month padding), date/time formatting in office timezone, sync status derivation.
    - `CalendarViewModelTest.kt`: Initial UI state, month summary mapping, status dot computation, month navigation (`onPreviousMonth`, `onNextMonth`, `onTodayClick`), day selection, punch sync status propagation, unverified badge handling, and sheet dismissal.

### Agent 8: Backend (NestJS)
- **Status:** Wave 1 Completed
- **Folder:** `server/`
- **Scope:** Auth module (JWT, bcrypt, rate limiting), Users & Photo upload (v1 / v2 one-time check), Attendance sync ingestion (idempotent punch_uuid, batch processing, day summary materialization), Monthly calendar endpoint, Model manifest endpoint, Config endpoint, Admin seed/CLI script, unit & E2E test suites.
- **Deliverables Completed:**
  - **Database & Entities (`server/src/database/entities/`):**
    - `UserEntity`: User profile with status enum, `photo_change_used` flag, password hash, and relation cascades.
    - `UserPhotoEntity`: Version constraint (1 or 2), unique constraint on `(user_id, version)`, sha256 hash, and active flag.
    - `OfficeConfigEntity`: Office coordinates (lat/lng), radius (m), timezone (`Asia/Kolkata`), sync windows JSON (`["16:00", "00:00"]`), jitter minutes (30), match threshold (0.55).
    - `SyncBatchEntity`: UUID batch audit logging with user relation, records count, and status enum (`SUCCESS`, `PARTIAL`, `FAILED`).
    - `AttendancePunchEntity`: Immutable punch record with UUID primary key, punch type (`IN`, `OUT`), UTC timestamp, time source (`NTP`, `DEVICE`), `time_unverified` flag, coordinates, distance, match score, image path, and sync batch relation.
    - `AttendanceDayEntity`: Materialized daily attendance summary with unique constraint on `(user_id, date)`, in/out times, status (`COMPLETE`, `MISSED_PUNCH`, `NO_RECORD`), and `time_unverified` flag.
  - **Authentication Module (`server/src/modules/auth/`):**
    - `POST /v1/auth/login`: Bcrypt password verification, login attempt rate limiting (in-memory throttler), JWT token generation (`sub`, `name`), user profile return with `photo_change_used` and `active_photo_version`.
    - `JwtStrategy` & `JwtAuthGuard`: Passport JWT token verification extracting user context.
    - Custom `@CurrentUser()` decorator.
  - **Office Configuration Module (`server/src/modules/config/`):**
    - `GET /v1/config`: Geofence office coordinates, radius, sync windows, jitter, timezone, and face match threshold (Bearer token protected).
  - **User & Photo Management Module (`server/src/modules/users/`):**
    - `POST /v1/users/me/photo`: Multipart onboarding profile photo v1 upload, validates single photo rule (rejects duplicate v1 with 400).
    - `PUT /v1/users/me/photo`: Profile photo replacement with version 2, strictly enforces one-time replacement rule (rejects 2nd change with HTTP 403 `PHOTO_CHANGE_EXHAUSTED`, rejects replacement if v1 missing with 400).
    - `GET /v1/users/me/photo`: Fetches active profile photo metadata, upload timestamp, sha256 checksum, and download URL (returns 404 if no photo).
  - **Attendance Ingestion & History Module (`server/src/modules/attendance/`):**
    - `POST /v1/attendance/sync`: Idempotent batch punch synchronization with multipart frame image ingestion; duplicate `punch_uuid` deduplication (`DUPLICATE`); automatic `attendance_days` daily summary calculation (first IN, last OUT, `COMPLETE` / `MISSED_PUNCH` status, `time_unverified` rollup); audit logging into `sync_batches`.
    - `GET /v1/attendance?month=YYYY-MM`: Monthly calendar history returning daily summary array and all punch records.
  - **Models Manifest Module (`server/src/modules/models/`):**
    - `GET /v1/models/manifest`: Public endpoint returning manifest for `det_10g`, `1k3d68`, `w600k_r50` ONNX models with file sizes, sha256 checksums, and download URLs.
  - **Admin CLI / Seed Script (`server/src/scripts/seed.ts`):**
    - Seeds default office config and test users (`EMP1001`, `EMP1002`, `EMP1003`, `ADMIN01`) with bcrypt-hashed passwords.
  - **Common Middleware & Filters:**
    - `HttpExceptionFilter`: Formats all errors to `{ code, message, details? }` strictly conforming to `ApiError` schema in OpenAPI.
    - Static file serving for `/uploads` (profile photos and punch images).
  - **Test Suite Results:**
    - `npm run build`: 100% clean compilation.
    - `npm test`: 11 test suites passed, 31 unit tests passed (100%).
    - `npm run test:e2e`: 1 test suite passed, 24 end-to-end tests passed (100%) covering auth, rate limiting, config, photo v1/v2/v3 business rules, attendance batch sync idempotency, duplicate punch handling, missed-punch daily calculation, and monthly history.

### Agent 9: QA and Integration Testing
- **Status:** Wave 3 Completed
- **Folder:** `docs/TEST_MATRIX.md`
- **Scope:** Manual Device Test Matrix, Cross-Cutting Integration Verification against Development Plan Sections 10 & 11 and PRD FR-01 through FR-34, Face Model Parity Testing, and Release Candidate Sign-Off.
- **Deliverables Completed:**
  - **Manual Device Test Matrix (`docs/TEST_MATRIX.md`):**
    - **Hardware Target Tiering:** Low-end baseline (2-3 GB RAM, Android 8.0/9.0, arm64-v8a, eMMC storage, aggressive battery killers) vs modern mid-range (6-8 GB RAM, Android 13/14, UFS 2.2+, fast inference).
    - **9 Complete Test Suites & 33 Test Scenarios:**
      1. *Authentication & Onboarding (TC-AUTH-01 to TC-AUTH-03, TC-ONBD-01 to TC-ONBD-05):* Valid/invalid login, token storage in Keystore, history pull, rate limiting (429), camera/location permissions, single-face validation, multi-face / no-face rejections, photo v1 upload.
      2. *Photo One-Time Change Rule (TC-PHOTO-01 to TC-PHOTO-04):* Change button visibility, photo v2 replacement, second replacement 403 `PHOTO_CHANGE_EXHAUSTED` rejection, dual photo database retention.
      3. *Geofence Verification (TC-GEO-01 to TC-GEO-05):* Inside 100m, outside 100m refusal with distance display, boundary tests (99m vs 101m), permission revocation, location services disabled.
      4. *Face Verification Pipeline (TC-FACE-01 to TC-FACE-06):* Cosine similarity match (>=0.55), mismatch refusal without consuming button, no-face / multi-face rejection, 3D landmark liveness / 2D screen spoof defense, sequential ONNX model memory isolation (peak RSS <= 180MB).
      5. *Punch State Machine & Midnight Reset (TC-STATE-01 to TC-STATE-05):* Punch In enabled / Out disabled -> In blocked / Out enabled -> Both blocked -> Midnight 00:00:00 rollover reset -> Next-day first punch marks prior incomplete day as `MISSED_PUNCH` with `out_time = in_time`.
      6. *Trusted Time & Offline Fallback (TC-TIME-01 to TC-TIME-03):* Online NTP in office timezone `Asia/Kolkata`, offline device clock fallback with `time_unverified = true` badge, monotonic elapsed time defense against clock tampering.
      7. *Sync Engine & Off-Peak Windows (TC-SYNC-01 to TC-SYNC-05):* Immediate "Sync Now", WorkManager scheduled window execution with bounded random jitter, server deduplication on `punch_uuid` (`DUPLICATE`), retry with exponential backoff, local image cleanup on acknowledgment.
      8. *Calendar & Day Details (TC-CAL-01 to TC-CAL-04):* 7-column grid, green complete dot, amber missed punch dot, month navigation, day detail bottom sheet with formatted office timestamps and unverified badges.
      9. *Edge, Stress & Environmental Scenarios (TC-EDGE-01 to TC-EDGE-06):* Airplane mode punch + subsequent sync, device reboot while offline with pending punches, app force-kill mid-sync, interrupted model download with HTTP Range resumption and SHA-256 integrity verification, low storage handling, timezone & DST handling.
  - **Cross-Cutting Integration Verification:**
    - Verified all 12 Acceptance Criteria from Section 10 of `Attendance_App_Development_Plan.md`.
    - Executed Face Pipeline Parity Tests via `python tools/parity_test.py` (Cosine similarity, Umeyama alignment transform, EAR/Yaw, nonrigid residual, weighted liveness score: 100% PASS).
    - Executed Backend E2E Test Suite via `npm run test:e2e` (24/24 tests PASS) & Unit Test Suite via `npm test` (31/31 tests PASS).
  - **QA Recommendation:** Zero blocking/critical defects. **APPROVED FOR RELEASE CANDIDATE (RC-1)**.

---

### Wave 4: Mobile UI Revamp (Findez Aesthetic Dark Theme)
- **Status:** Wave 4 Completed
- **Branch:** `ui-revamp`
- **Scope:** Complete design transformation of the Android application based on Dribbble "Findez – Digital Attendance App".
- **Deliverables Completed:**
  - **Phase A1: Design System Foundation:**
    - Custom dark theme tokens: `AppBackground` (`#070B0F`), `AppHeaderBackground` (`#14181D`), `AppSurface` (`#14181D`), `AppAccent` (`#4EFFCA` mint), `AppAccentTint` (`0x244EFFCA`), `AppOnAccent` (`#04120D`), `AppDisabled` (`#0C1015`), `AppBorder` (`#2A2E33`), `AppWarning` (`#FBBF24`), `AppError` (`#F87171`).
    - Bundled Google Fonts Poppins family (`poppins_regular.ttf`, `poppins_medium.ttf`, `poppins_semibold.ttf`, `poppins_bold.ttf`).
    - Updated typography (`ClockTime` 48sp, `StatValue` 22sp, `StatLabel` 13sp, `RingCaption` 14sp, `NavLabel` 12sp).
    - Updated corner radii and dimensions: Pill buttons (26dp radius, 52dp height), card radius 20dp, stat tiles 20dp, body panel top corners 32dp, 44dp icon circle button.
    - Updated `ThemeTokensTest.kt`: 100% PASS.
  - **Phase A2: Hand-Traced Vector Drawables:**
    - 14 crisp vector drawables in `res/drawable/` on 24dp grid with 1.5–2dp stroke: `ic_findez_check_in`, `ic_findez_check_out`, `ic_findez_checked_box`, `ic_findez_calendar_dots`, `ic_findez_calendar_8`, `ic_findez_menu_four_dots`, `ic_findez_nav_home`, `ic_findez_nav_home_active`, `ic_findez_nav_schedule`, `ic_findez_nav_schedule_active`, `ic_findez_nav_settings`, `ic_findez_nav_settings_active`, `ic_findez_punch_in`, `ic_findez_punch_out`.
  - **Phase A3: Restyled Core Components:**
    - Pill mint primary button with dark text (`#04120D`), outlined secondary button on dark surface, restyled danger button.
    - Day cell selected mint filled circle; today mint outline.
    - Dialogs with 28dp radius surface.
  - **Phase A4: New Findez Components:**
    - `IconCircleButton`: 44dp circular button with 1dp border.
    - `SectionHeader`: Clean heading with optional text action button ("See more").
    - `StatCard`: 20dp tile with icon top-right, value + label, tap-to-refresh spinner.
    - `WorkRing`: 200dp ring, 14dp stroke, round caps, 4 states (`NotStarted`, `InProgress`, `Complete`, `MissedPunch`), trusted clock caption, shift chip.
    - `SwipeToPunch`: 64dp pill, 52dp thumb, haptic feedback, TalkBack support, pure tested threshold logic (`SwipeToPunchThresholdTest`: 100% PASS).
    - `WeekStrip`: Horizontally scrollable day circles with day numbers over weekday letters and status dots.
  - **Phase A7: Nav Shell & Center Button:**
    - 4 slots on bottom bar: Home | Calendar | raised 56dp center punch button protruding 18dp | Settings.
    - Center punch button triggers punch on Home, navigates to Home on Calendar/Settings.
    - Admin panel moved inside Settings only.
    - Updated `NavigationRoutesTest.kt`: 100% PASS.
  - **Phase A5: Home Screen Revamp:**
    - Created pure `WorkDurationCalculator` (+ comprehensive unit test: 100% PASS).
    - Updated `HomeUiState` & `HomeViewModel`: derives duration, ring progress, center punch state (IN/OUT/DONE/BLOCKED), and monthly attendance count.
    - Revamped `HomeScreen`: Two-tone layout (header `#14181D`, body `#070B0F` with 32dp top corners), 2×2 stat grid, WorkRing, SyncStatusBar, pinned SwipeToPunch slider.
    - `HomeViewModelTest.kt`: 100% PASS.
  - **Phase A6: Calendar Screen Revamp:**
    - Additive `CalendarUiState` fields (`isMonthViewExpanded`, `summariesMap`).
    - Enhanced `CalendarViewModel` (`toggleMonthView`, `selectDate`, `openDetailSheet`) with month-boundary selection and summary loading.
    - Revamped `CalendarScreen`: Header zone with circular 4-dots toggle, formatted month + date, circular today button; collapsible `HorizontalPager` week strip; expanded month grid; "Your Attendance" section with Check In / Check Out cards and unverified time badge; `DayDetailBottomSheet` integration.
    - `CalendarViewModelTest.kt` & `CalendarDateUtilsTest.kt`: 100% PASS.
  - **Phase A8: Other Screens & Contrast:**
    - Login: Dark hero layout with mint focus rings and pill primary button.
    - Profile/Settings: Verified theme tokens, replaced `Color.White` with theme tokens.
    - Punch Capture: Oval guide and progress ring turned mint accent (`#4EFFCA`), hardcoded colors replaced with tokens.
    - Onboarding: Camera shutter button and progress indicators aligned to mint accent and tokens.
    - Admin: FloatingActionButton and chips updated to `AppOnAccent` text on `AppAccent` for high contrast.
    - Component Gallery: Added sections 12 & 13 previewing all new Findez components and 14 vector drawables.
  - **Phase A9: QA & Verification:**
    - Zero stray `Color(0x...)` literals outside `design/theme/`.
    - 100% passing UI Revamp unit test suites (`ThemeTokensTest`, `NavigationRoutesTest`, `SwipeToPunchThresholdTest`, `WorkDurationCalculatorTest`, `HomeViewModelTest`, `CalendarViewModelTest`, `CalendarDateUtilsTest`).
    - Full application `assembleDebug` APK build: **BUILD SUCCESSFUL**.

  - **Phase A10: UI & Architecture Review Patch (Production Hardening & Anti-Slop):**
    - **Production Backend URL:** Set production endpoint `https://attendance.pess.co.in/` as default fallback in `BackendConfig.kt` and `build.gradle.kts`.
    - **Global Punch Coordinator:** Implemented `PunchCoordinator` to trigger geofence checks and directly dispatch `GlobalPunchEvent.NavigateToCapture` from ANY tab (Home, Calendar, Settings) rather than just redirecting to Home.
    - **Single Profile & Settings Tab:** Maintained the unified `Profile & Settings` screen per user instruction.
    - **Anti-Slop Cleanups:**
      - Removed ticking office-time captions and shift chip clutter from `WorkRing`.
      - Redesigned `StatCard` to exact Findez reference: bold Value top-left, clean Icon top-right, minimal Label bottom-left (eliminated subtitle clutter like "16 m away" / "This month").
      - Unpinned `SwipeToPunch` from bottom overlay Box in `HomeScreen`, nesting it smoothly inside the scrollable column to prevent overlap over cards and `SyncStatusBar`.
    - **Calendar Streamlining:** Removed status dots from `WeekStrip` and `CalendarDayCell`, and removed the redundant `CalendarLegend`.
    - **Admin Screen:** Replaced oversized green pill site card with a sleek `DropdownMenu` selector styled with `AppSurface` and `AppBorder`.
    - **Comprehensive Verification:** Unit tests for `PunchCoordinatorTest` and all revamped components 100% passing; debug APK build fully successful.

  - **Phase A11: Production Connection & Polish Patch:**
    - **Fixed Localhost / 172.31 Timeout:** Updated `local.properties`, `build.gradle.kts`, and `BackendConfig.kt` to ensure `https://attendance.pess.co.in/` is baked into `BuildConfig.SERVER_BASE_URL` and never overridden by stale emulator / local IP configs. Guarded `HostFallbackInterceptor` against local fallback on HTTPS / production domains.
    - **Cleaned Calendar Expanded View:** Removed status dots from `CalendarDayCell.kt` so days render cleanly centered inside circular cells without dots. Removed the bottom `MonthlySummaryCard` pill and `CalendarLegend` from `CalendarScreen.kt` to match Findez minimal style.
    - **Cleaned Pending Sync / Sync Now:** Restyled `SyncStatusBar.kt` into a sleek, compact status pill that returns early and renders **nothing** when `pendingCount == 0 && !isSyncing`, completely removing the AI-slop banner when synced.
    - **Verification:** Unit test suite 100% PASS; `assembleDebug` built successfully with production URL.

  - **Phase A12: Multi-Month Data Sync, Camera Ergonomics, 5-Slot Bottom Bar, and Admin De-Slop:**
    - **Multi-Month Attendance Preloading & Photo Cache on Login:**
      - Updated `LoginViewModel.kt` to asynchronously loop through 4 months (`0L..3L`: current month + past 3 months) to fetch and upsert attendance into Room DB.
      - Fixed `TokenStorageImpl` in-memory token retrieval and synchronous commit to avoid race conditions.
      - Added profile photo download from `/users/me/photo` with graceful error logging and embedding generation.
      - Provided default fallback values in `PhotoDetailResponseDto` and `PhotoUploadResponseDto` so deserialization never fails.
    - **Camera-Style Punch Capture HUD (All 3 Bottom Controls):**
      - Restructured `PunchCaptureScreen.kt` controls deck: all 3 controls are positioned side-by-side at the bottom for one-handed thumb reach (Left: Screen Flash toggle glass button, Center: 76dp dual-ring mint shutter trigger, Right: Close/Cancel glass button).
    - **5-Slot Bottom Navigation with Tracker ("Coming Soon"):**
      - Created Findez vector drawables `ic_findez_nav_tracker.xml` and `ic_findez_nav_tracker_active.xml`.
      - Added `BottomNavItem.TRACKER` and `Screen.Tracker` (route `"tracker"`).
      - Built `TrackerScreen.kt` featuring a glowing activity dashboard with "COMING SOON" badge and upcoming feature preview cards.
    - **Calendar Double-Menu Elimination:**
      - Updated `CalendarViewModel.kt` so `onDaySelected` strictly updates inline attendance cards (`selectDate(day.date, openSheet = false)`).
      - The `DayDetailBottomSheet` opens strictly when the user taps "See more".
    - **Minimalist StatCards & Unclickable Home Profile Avatar:**
      - Stripped background circle from `StatCard.kt`, presenting clean vector outlines directly on dark cards per Findez closeup reference.
      - Removed click navigation on Home profile avatar.
    - **Admin Screen Complete De-Slop:**
      - Removed FloatingActionButton. Placed clean "+ Add Staff" action in top bar.
      - Added 2-tile metrics overview at top of Sites MenuView (`StatCard`: Office Sites, Enrolled Staff).
      - In `SiteCardItem`: Clean site name, active staff count, geofence radius. Removed developer jargon (`Threshold: 0.38`).
      - In `UserItemCard`: Replaced jammed row of 3 buttons (`Change Site`, `Password`, `Delete`) with a 3-dot `DropdownMenu` (`MoreVert`).
      - In `CreateSiteDialog`: Removed debug policy note box.
    - **Verification & Build:**
      - Unit test suites (`NavigationRoutesTest`, `ThemeTokensTest`, `SwipeToPunchThresholdTest`, `WorkDurationCalculatorTest`, `HomeViewModelTest`, `CalendarViewModelTest`, `LoginViewModelTest`) all 100% PASS.
      - `assembleDebug` APK build: **BUILD SUCCESSFUL**. Output: `android/app/build/outputs/apk/debug/app-debug.apk`.

  - **Phase S1-S6: Profile Photos Stored in Database & Punch Image Deprecation:**
    - **Database Storage for Profile Photos:** Added dialect-aware binary column `image_data` (`varbinary(max)` for MSSQL, `longblob` for MySQL, `blob` for SQLite) and `mime_type` varchar(32) to `user_photos` entity, excluded from default selects.
    - **Safe Database Migration:** Created `server/src/database/migrations/002_store_profile_photos_in_db.sql` for MSSQL with idempotent ALTER TABLE checks and MySQL equivalents documented.
    - **Upload & File Serving Endpoints:**
      - Updated `uploadPhotoV1` and `replacePhotoV2` to validate magic bytes (JPEG/PNG only) and 5 MB max size, storing bytes directly into DB with sha256 hash. Removed all disk file writing.
      - Removed `PHOTO_ALREADY_EXISTS` blockage for legacy rows missing image data: legacy rows are cleared, allowing smooth re-enrolment.
      - Added `GET /v1/users/me/photo/file` returning active photo bytes with HTTP ETag caching (304 Not Modified).
      - Set stored `image_path` to `/v1/users/me/photo/file` for seamless backward compatibility.
    - **Auth & Admin Enhancements:**
      - Login `has_photo` and `active_photo_version` now strictly require an active row with image data.
      - Added Admin photo reset endpoint `POST /v1/admin/users/:userId/photo/reset` and `GET /v1/admin/users/:userId/photo`.
      - Added "Reset Photo" action with confirmation dialog to the Admin Web UI.
    - **Punch Images Deprecated:** Removed punch disk folder writes and file saving during sync. Punches now cleanly store `image_path = null` while maintaining backward-compatible DTO acceptance.
